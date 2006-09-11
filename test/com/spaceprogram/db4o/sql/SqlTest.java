package com.spaceprogram.db4o.sql;

import com.db4o.ObjectContainer;
import com.db4o.reflect.generic.GenericObject;
import com.db4o.reflect.generic.GenericClass;
import com.db4o.reflect.generic.GenericReflector;
import com.db4o.reflect.generic.GenericField;
import com.db4o.reflect.jdk.JdkReflector;
import com.db4o.reflect.ReflectField;
import com.db4o.query.Query;
import com.spaceprogram.db4o.Contact;
import com.spaceprogram.db4o.TestUtils;
import com.spaceprogram.db4o.sql.parser.SqlParseException;
import org.junit.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Date;

/**
 * User: treeder
 * Date: Jul 10, 2006
 * Time: 2:12:45 PM
 */
public class SqlTest extends ContactTest{

    /**
     * Will run a query for an object that is not in classpath.
     * Need to make GenericObjects here with no class on hand.
     */
    @Test
    public void testNoClass() throws Sql4oException, SqlParseException {

        initGenericObjects();

        List<Result> results = Sql4o.execute(oc, "FROM com.acme.Person");
        System.out.println("Results.size: " + results.size());
        TestUtils.displaySqlResults(results);
    }

    private void initGenericObjects() {
        GenericClass personClass = initGenericClass();
        ReflectField surname = personClass.getDeclaredField("surname");
        ReflectField birthdate = personClass.getDeclaredField("birthdate");

        Object person = personClass.newInstance();
        surname.set(person, "John");
        birthdate.set(person, new Date());
        // todo: this doesn't work
        //oc.set(person);
    }

    private GenericClass initGenericClass() {
        GenericReflector reflector = new GenericReflector(null, new JdkReflector(Thread.currentThread().getContextClassLoader()));
        GenericClass _objectIClass = (GenericClass)reflector.forClass(Object.class);
        GenericClass result = new GenericClass(reflector, null, "com.acme.Person", _objectIClass);
        result.initFields(fields(result, reflector));
        return result;
    }

    private GenericField[] fields(GenericClass personClass, GenericReflector reflector) {
        return new GenericField[] {
                new GenericField("surname", reflector.forClass(String.class), false, false, false),
                new GenericField("birthdate", reflector.forClass(Date.class), false, false, false),
                new GenericField("bestFriend", personClass, false, false, false)
        };
    }



    /**
     * - test query time vs normal soda query
     * - test that correct number of results are returned
     * - maybe correct value too
     *
     * @throws SQLException
     */
    @Test
    public void testQueryResults() throws SQLException, SqlParseException, ClassNotFoundException, Sql4oException {

        // todo: assert that soda results equal sql results
        int sodaCount = 0;
        // lets time a sode query vs the jdbc
        {
            System.out.println("Soda query...");
            ObjectContainer oc = server.openClient();
            Query q = oc.query();
            q.constrain(Contact.class);
            q.descend("name").constrain("contact 2");
            q.descend("category").constrain("friends");
            long startTime = System.currentTimeMillis();
            List results = q.execute();
            for (Object o : results) {
                Contact c = (Contact) o;
                System.out.println("got: " + c);
                sodaCount++;
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("soda duration: " + duration);
            oc.close();
        }

        int sqlCount = 0;
        {
            // now same query with sql
            System.out.println("SQL query");
            ObjectContainer oc = server.openClient();
            try {
                long startTime = System.currentTimeMillis();

                // execute query
                String query = "select * from com.spaceprogram.db4o.Contact c where " +
                        " name = 'contact 2' and " + //  and email = 'email@2.com'
                        " category = 'friends'";

                List<Result> results = Sql4o.execute(oc, query);
                sqlCount = results.size();
                TestUtils.displaySqlResults(results);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.println("SQL duration: " + duration);
            } finally {
                oc.close();
            }
        }
        Assert.assertEquals(sodaCount, sqlCount);
    }

    @Test
    public void testSelectFieldsQuery() throws SqlParseException, Sql4oException {
        String query = "select name, age from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(1, results.size());
        // get by index
        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject(0));

        // get by name
        Assert.assertEquals(20, result.getObject("age"));
    }

    @Test(expected = Sql4oRuntimeException.class)
    public void testFieldExceptions() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "select name, age from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Result result = results.get(0);
        Object somefield = result.getObject("somefield"); // this should throw, but it's expected
    }

    @Test
    public void testAsteriskQuery() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "select * from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(1, results.size());
        // get by index
        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject("name"));

        // get by name
        Assert.assertEquals(20, result.getObject("age"));
    }

    @Test
    public void testNoSelectQuery() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "name = 'contact 2' and " + //  and email = 'email@2.com'
                " category = 'friends'";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(1, results.size());

        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject("name"));

        Assert.assertEquals(20, result.getObject("age"));
    }
    @Test
    public void testIntegerCondition() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                " age = 10 or age = 20 ";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(2, results.size());

        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject("name"));

        Assert.assertEquals(20, result.getObject("age"));
    }
    @Test
    public void testDoubleCondition() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "age = 20 and " +
                "income = 50000.02";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(1, results.size());

        Result result = results.get(0);
        Assert.assertEquals("contact 2", result.getObject("name"));
        Assert.assertEquals(50000.02, result.getObject("income"));
    }
    @Test
    public void testComplexWhere1() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "(age = 10 or age = 20) and " +
                "income = 50000.02";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(2, results.size());

        Result result = results.get(0);
        Assert.assertEquals(50000.02, result.getObject("income"));
    }
    @Test
    public void testLessThan() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "income < 50000.03";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(5, results.size());
    }
    @Test
    public void testLessThanOrEqual() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "income <= 50000.02";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(5, results.size());
    }
    @Test
    public void testLessThanOrEqual2() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "income <= 50000.01";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(0, results.size());
    }
    @Test
    public void testGreaterThan() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "income > 50000.03";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(5, results.size());
    }
    @Test
    public void testGreaterThanOrEqual() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "income >= 50000.02";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(10, results.size());
    }
    @Test
    public void testGreaterThanOrEqual2() throws SqlParseException, ClassNotFoundException, Sql4oException {
        String query = "from com.spaceprogram.db4o.Contact c where " +
                "income >= 50000.03";

        List<Result> results = Sql4o.execute(oc, query);
        TestUtils.displaySqlResults(results);

        Assert.assertEquals(5, results.size());
    }
}