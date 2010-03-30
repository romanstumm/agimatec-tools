package com.agimatec.dbmigrate;

import com.agimatec.commons.beans.MapQuery;
import com.agimatec.commons.config.*;
import com.agimatec.commons.util.FileUtils;
import com.agimatec.commons.util.PropertyReplacer;
import com.agimatec.database.DbUnitDumpTool;
import com.agimatec.database.DbUnitSetupTool;
import com.agimatec.dbmigrate.groovy.GroovyScriptTool;
import com.agimatec.dbmigrate.util.*;
import com.agimatec.jdbc.JdbcConfig;
import com.agimatec.jdbc.JdbcDatabase;
import com.agimatec.jdbc.JdbcDatabaseFactory;
import com.agimatec.jdbc.JdbcException;
import com.agimatec.sql.meta.checking.DatabaseSchemaChecker;
import com.agimatec.sql.script.SQLScriptExecutor;
import com.agimatec.sql.script.SQLScriptParser;
import com.agimatec.sql.script.ScriptVisitor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.*;

/**
 * <p>Title: Agimatec GmbH</p>
 * <p>Description: Base class for Migration and Testcases.
 * Provides infrastructure and common utilities.</p>
 * <p>development: 2007-2010</p>
 *
 * @author Roman Stumm
 */
public abstract class BaseMigrationTool implements MigrationTool {
  protected static final Log log = LogFactory.getLog("agimatec.migration");

  protected JdbcDatabase targetDatabase;
  protected String migrateConfigFileName = "migration.xml";
  protected final DBVersionMeta dbVersionMeta = new DBVersionMeta();
  private String scriptsDir;

  public BaseMigrationTool() {
  }

  public void setUp() {
    setupVersionMeta();
  }

  protected void setupVersionMeta() {
    MapNode versionMeta = (MapNode) getMigrateConfig().get("version-meta");
    if (versionMeta != null) {
      if (versionMeta.get("table") != null) {
        dbVersionMeta.setTableName(versionMeta.getString("table"));
      }
      if (versionMeta.get("version") != null) {
        dbVersionMeta.setColumn_version(versionMeta.getString("version"));
      }
      if (versionMeta.get("since") != null) {
        dbVersionMeta.setColumn_since(versionMeta.getString("since"));
      }
      if (versionMeta.get("insert-only") != null) {
        dbVersionMeta.setInsertOnly(versionMeta.getBoolean("insert-only"));
      }
      if (versionMeta.get("auto-create") != null) {
        dbVersionMeta.setAutoCreate(versionMeta.getBoolean("auto-create"));
      }
      if (versionMeta.get("auto-version") != null) {
        dbVersionMeta.setAutoVersion(versionMeta.getBoolean("auto-version"));
      }
    }
  }

  public void tearDown() throws Exception {
    terminateTransactions();
    disconnectDatabase();
  }

  /**
   * callback -
   */
  public void halt(String message) {
    final String date = DateFormat.getDateTimeInstance().format(new Date());
    throw new HaltedException("++ HALT at " + date + "! ++ " + message);
  }

  /**
   * callback - update the version
   *
   * @throws SQLException
   */
  public void version(String dbVersion) throws SQLException {
    UpdateVersionScriptVisitor
        .updateVersionInDatabase(targetDatabase, dbVersion, dbVersionMeta);
  }

  /**
   * execute the content of a file as a single SQL statement.
   * You can use this, when you need not parse the file or when the file cannot be parsed.
   * Example: use this to execute a PL/SQL package, that is stored in a single file (1 file for the spec,
   * 1 file for the body).
   *
   * @param scriptName - may contain properties,
   *                   but not supported are: -- #if conditions, reconnect, subscripts etc.
   * @throws SQLException
   * @throws IOException
   */
  public void execSQLScript(String scriptName) throws SQLException, IOException {
    ScriptVisitor visitor = new SQLScriptExecutor(targetDatabase);
    boolean failOnError = true;

    SQLScriptParser parser = new SQLScriptParser(getScriptsDir(), getLog());
    parser.setEnvironment(getEnvironment());
    parser.setFailOnError(failOnError); // if error occurs, do NOT continue!
    parser.execSQLScript(visitor, scriptName);
  }

  /**
   * callback - parse the script and execute each SQL statement
   *
   * @throws IOException
   * @throws SQLException
   */
  public void doSQLScript(String scriptName) throws IOException, SQLException {
    iterateSQLScript(new SQLScriptExecutor(targetDatabase), scriptName, true);
  }

  /**
   * callback - parse the script and execute each SQL statement
   *
   * @throws Exception
   */
  public void doSQLScriptIgnoreErrors(String scriptName) throws Exception {
    iterateSQLScript(new SQLScriptExecutor(targetDatabase), scriptName, false);
  }

  /**
   * callback - invoke DatabaseSchemaChecker for invalid triggers, views, ...
   */
  public void checkObjectsValid(String databaseType) throws Exception {
    DatabaseSchemaChecker checker = DatabaseSchemaChecker.forDbms(databaseType);
    checker.setDatabase(getTargetDatabase());
    checker.assertObjectsValid();
  }

  /**
   * callback - invoke DatabaseSchemaChecker for completeness of
   * schema (columns, tables, foreign keys, indices, ...)
   *
   * @throws Exception
   */
  public void checkSchemaComplete(String dev) throws Exception {
    StringTokenizer t = new StringTokenizer(dev, ",");
    String databaseType = t.nextToken();
    String configKey = t.nextToken();
    DatabaseSchemaChecker checker = DatabaseSchemaChecker.forDbms(databaseType);
    checker.setDatabase(getTargetDatabase());
    List<URL> urls = getURLsFromEnv(configKey);
    checker.checkDatabaseSchema(urls.toArray(new URL[urls.size()]));
  }

  private List<URL> getURLsFromEnv(String configKey) throws MalformedURLException {
    List files = (List) getEnvironment().get(configKey);
    Iterator<String> it = files.iterator();

    List<URL> urls = new ArrayList();
    while (it.hasNext()) {
      urls.add(ConfigManager.toURL(it.next()));
    }
    return urls;
  }

  /**
   * callback - invoke dbunit
   *
   * @param files - comma-separated delete and insert DB-Unit script,
   *              example: "delete_data.xml,data.XML"
   *              example: "data.XML"
   * @throws Exception
   */
  public void dbSetup(String files) throws Exception {
    DbUnitSetupTool tool = new DbUnitSetupTool();
    invokeBeanCallbacks(tool);
    String[] parts = files.split(",");
    if (parts.length == 2) {
      tool.setDeleteDataFile(parts[0]);
      tool.setDataFile(parts[1]);
    } else if (parts.length == 1) {
      tool.setDeleteDataFile(null);
      tool.setDataFile(parts[0]);
    }
    tool.execute();
  }

  /**
   * copy the files from source to target
   *
   * @param configKey - key in the env, value is list of source_1, target_1, ... URLs
   * @throws IOException
   */
  public void copyFiles(String configKey) throws IOException {
    List<URL> urls = getURLsFromEnv(configKey);
    Iterator<URL> it = urls.iterator();
    while (it.hasNext()) {
      URL source = it.next();
      URL target = it.next();
      File ftarget = FileUtils.toFile(target);
      log("Copy " + source + " --> " + ftarget);
      try {
        FileUtils.copyURLToFile(source, ftarget);
      } catch (FileNotFoundException ex) {
        log.info("file to backup-copy not found, source: " + source + " target: " + ftarget);
        if (ftarget.exists()) ftarget.delete(); // delete target when source not exists
      }
    }
  }

  /**
   * callback - invoke dbunit
   * example: "data.XML"
   *
   * @throws Exception
   */
  public void dbDump(String file) throws Exception {
    DbUnitDumpTool tool = new DbUnitDumpTool();
    invokeBeanCallbacks(tool);
    tool.setDataFile(file);
    tool.execute();
  }

  /**
   * invoke a static n-arg-method on a class.
   * All parameters of the target method must be of type String!!
   *
   * @throws Exception
   */
  public void invokeStatic(String classMethod) throws Exception {
    invokeClassMethod(true, classMethod);
  }

  /**
   * invoke a n-arg-method on a new instance of a class.
   * All parameters of the target method must be of type String!!
   *
   * @throws Exception
   */
  public void invokeBean(String classMethod) throws Exception {
    invokeClassMethod(false, classMethod);
  }

  protected void invokeClassMethod(boolean isStatic, String classMethod)
      throws Exception {
    Object[] args = splitMethodArgs(classMethod);
    Class clazz = Class.forName((String) args[0]);
    Method m;
    Object receiver = null;
    if (!isStatic) {
      receiver = clazz.newInstance();
      invokeBeanCallbacks(receiver);
    }
    if (args[2] == null) {
      m = clazz.getMethod((String) args[1]);
      m.invoke(receiver);
    } else {
      List params = ((List) args[2]);
      m = findMethod(clazz, (String) args[1], params.size());
      m.invoke(receiver, params.toArray());
    }
  }

  /**
   * callback - invoke a groovy script
   *
   * @throws IOException
   */
  public void doGroovyScript(String scriptInvocation) throws Exception {
    GroovyScriptTool tool = new GroovyScriptTool();
    invokeBeanCallbacks(tool);
    List<String> params = splitParams(scriptInvocation);
    int idx = scriptInvocation.indexOf("(");
    if (idx > 0) scriptInvocation = scriptInvocation.substring(0, idx);
    tool.getBinding().setVariable("params", params);
    tool.start(scriptInvocation);
  }

  protected void invokeBeanCallbacks(Object receiver) {
    if (receiver instanceof MigrationToolAware) {
      ((MigrationToolAware) receiver).setMigrationTool(this);
    }
  }

  protected Method findMethod(Class clazz, String methodName, int paramCount) {
    for (Method m : clazz.getMethods()) {
      if (m.getName().equals(methodName) &&
          m.getParameterTypes().length == paramCount) {
        return m;
      }
    }
    return null;
  }

  protected Object[] splitMethodArgs(String classMethod) {
    int pos = classMethod.lastIndexOf('#');
    String clazzName = classMethod.substring(0, pos);
    String methodName = classMethod.substring(pos + 1);
    pos = methodName.indexOf('(');
    if (pos > 0) {
      methodName = methodName.substring(0, pos);
    }
    return new Object[]{clazzName, methodName, splitParams(classMethod)};
  }

  protected List<String> splitParams(String methodName) {
    int pos = methodName.indexOf('(');
    List<String> params = null;
    if (pos > 0) {
      StringTokenizer paramTokens =
          new StringTokenizer(methodName.substring(pos + 1), "(,)");
      params = new ArrayList();
      while (paramTokens.hasMoreTokens()) {
        params.add(paramTokens.nextToken());
      }
    }
    return params;
  }

  /**
   * iterate sql script.
   */
  protected void iterateSQLScript(ScriptVisitor visitor, String scriptName,
                                  boolean failOnError)
      throws IOException, SQLException {
    SQLScriptParser parser = new SQLScriptParser(getScriptsDir(), getLog());
    Map env;
    parser.setEnvironment(env = getEnvironment());
    parser.setFailOnError(failOnError); // if error occurs, do NOT continue!

    visitor = new ReconnectScriptVisitor(targetDatabase, visitor);
    visitor = new SubscriptCapableVisitor(visitor, parser);
    visitor = new UpdateVersionScriptVisitor(targetDatabase, visitor, dbVersionMeta);
    visitor = new ConditionalScriptVisitor(visitor,
        env); // must be outer visitor to prevent execution in case of false-conditions

    parser.iterateSQLScript(visitor, scriptName);
  }

  public DBVersionMeta getDbVersionMeta() {
    return dbVersionMeta;
  }

  /**
   * a map for environment properties (defaults are JVM System-Properties)
   * (to be used to conditional execution of statements in
   * SQL scripts)
   *
   * @return a map (do not modify!)
   */
  public Map getEnvironment() {
    Map m = getMigrateConfig().getMap("env");
    final Map env;
    if (m == null) {
      Map p = new Properties(System.getProperties());
      getMigrateConfig().put("env", p);
      env = p;
    } else if (m instanceof Properties) {
      env = m;
    } else {
      Properties p = new Properties();
      p.putAll(System.getProperties());
      p.putAll(m);
      getMigrateConfig().put("env", p);
      env = p;
    }
    replaceProperties(env);
    return env;
  }

  protected void replaceProperties(Map env) {
    replaceInMap(new PropertyReplacer(env), env);
  }

  private void replaceInMap(PropertyReplacer replacer, Map map) {
    for (Object entry : map.entrySet()) {
      Object value = ((Map.Entry) entry).getValue();
      value = replaceValue(replacer, value);
      if (value != null) {
        ((Map.Entry) entry).setValue(value);
      }
    }
  }

  private void replaceInList(PropertyReplacer replacer, List list) {
    int idx = 0;
    for (Object each : list) {
      each = replaceValue(replacer, each);
      if (each != null) {
        list.set(idx, each);
      }
      idx++;
    }
  }

  private Object replaceValue(PropertyReplacer replacer, Object value) {
    if (value instanceof String) {
      return replacer.replaceProperties((String) value);
    } else if (value instanceof Map) {
      replaceInMap(replacer, (Map) value);
    } else if (value instanceof List) {
      replaceInList(replacer, (List) value);
    } else if (value instanceof ListNode) {
      replaceInList(replacer, ((ListNode) value).getList());
    } else if (value instanceof MapNode) {
      replaceInMap(replacer, ((MapNode) value).getMap());
    }
    return null;
  }

  public JdbcDatabase getTargetDatabase() {
    return targetDatabase;
  }

  public Config getMigrateConfig() {
    return ConfigManager.getDefault()
        .getConfig("migration", getMigrateConfigFileName());
  }

  private String getMigrateConfigFileName() {
    return migrateConfigFileName;
  }

  public void setMigrateConfigFileName(String migrateConfigFileName) {
    this.migrateConfigFileName = migrateConfigFileName;
  }

  protected List getOperations(String name) {
    return getMigrateConfig().getList("Operations/" + name);
  }

  public String getScriptsDir() {
    if (scriptsDir == null) {
      FileNode dir = (FileNode) getMigrateConfig().get("Scripts");
      if (dir == null) return null;
      scriptsDir = dir.getFilePath();
    }
    return scriptsDir;
  }

  public void setScriptsDir(String scriptsDir) {
    this.scriptsDir = scriptsDir;
  }

  protected void perform(List operations) throws Exception {
    if (operations == null) {
      return;
    }
    for (Object each : operations) {
      if (each instanceof TextNode) {
        TextNode node = (TextNode) each;
        doMethodOperation(node.getName(), node.getValue());
      } else if (each instanceof ListNode) {
        MapQuery q = new MapQuery(((ListNode) each).getName());
        boolean isTrue = q.doesMatch(getEnvironment());
        print("FOUND Condition: (" + q.toString() + ") = " + isTrue);
        if (isTrue) {
          perform(((ListNode) each).getList()); // recursion!
          print("END of Condition: (" + q.toString() + ")");
        }
      }
    }
  }

  public void doMethodOperation(String methodName, String methodParam)
      throws Exception {
    print("Next operation: " + methodName + "(\"" + methodParam + "\")");
    Method method = getClass().getMethod(methodName, String.class);
    try {
      method.invoke(this, methodParam);
    } catch (InvocationTargetException tex) {
      rollback();
      log(tex.getTargetException());
      throw (Exception) tex.getTargetException();
    }
  }

  public void connectTargetDatabase() {
    if (targetDatabase == null) {
      String dbFile = getJdbcConfigFile();
      JdbcConfig databaseConfig = new JdbcConfig();
      if (dbFile != null) {
        print("connect to jdbc using " + dbFile + "...");
        databaseConfig.read(dbFile);
      }
      applyEnvironment(databaseConfig);
      targetDatabase = createDatabase(databaseConfig);

      if (getTargetDatabase() != null) {
        getTargetDatabase().begin();
      }
    }
  }

  protected JdbcDatabase createDatabase(JdbcConfig databaseConfig) {
    JdbcDatabase aDatabase = JdbcDatabaseFactory.createInstance(databaseConfig);
    return aDatabase;
  }

  /**
   * ensure that the env-variables are used immediately to connect the database!
   */
  protected void applyEnvironment(JdbcConfig jdbcConfig) {
    Map env = getEnvironment();
    Object v = env.get("DB_USER");
    if (v != null) {
      jdbcConfig.getProperties().put("user", v);
    }
    v = env.get("DB_PASSWORD");
    if (v != null) {
      jdbcConfig.getProperties().put("password", v);
    }
    v = env.get("DB_SCHEMA");
    if (v != null) {
      String urlConnect = ReconnectScriptVisitor
          .replaceJdbcSchemaName(jdbcConfig.getConnect(), (String) v);
      jdbcConfig.setConnect(urlConnect);
    }
    v = env.get("DB_URL");
    if (v != null) {
      jdbcConfig.setConnect((String) v);
    }
    v = env.get("DB_DRIVER");
    if (v != null) {
      jdbcConfig.setDriver((String) v);
    }
  }

  protected void commit() {
    try {
      getTargetDatabase().getConnection().commit();
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
    log.info("** commit **");
  }

  protected String getJdbcConfigFile() {
    return getMigrateConfig().getURLPath("JdbcConfig");
  }

  public void print(Object obj) {
    System.out.println(obj);
    log(obj);
  }

  public void log(Object obj) {
    if (obj instanceof Throwable) {
      getLog().error(null, (Throwable) obj);
    } else {
      getLog().info(obj);
    }
  }

  public Log getLog() {
    return log;
  }

  public void rollback() throws Exception {
    try {
      log("** rollback **");
      if (getTargetDatabase() == null) {
        return;
      }
      if (getTargetDatabase().isTransaction()) {
        getTargetDatabase().rollback();
      }
    } catch (Exception ex) {
      getLog().error(null, ex);
    }
  }

  public void terminateTransactions() throws Exception {
    if (this.targetDatabase != null) {
      if (getTargetDatabase().isTransaction()) {
        try {
          getTargetDatabase().commit();
        } catch (Exception ex) {
          getLog().error(null, ex);
        }
      }
    }
  }

  public void disconnectDatabase() throws Exception {
    if (targetDatabase != null) {
      targetDatabase.close();
      targetDatabase = null;
    }
  }

  /**
   * overwrite in subclasses
   */
  protected boolean acceptDirectoryForSQLParser(File aDirectory) {
    return (!aDirectory.getName().equalsIgnoreCase("packages") &&
        !aDirectory.getName().equalsIgnoreCase("triggers"));
  }

  /**
   * utility method to exec a JDBC SELECT statement directly
   *
   * @throws SQLException
   */
  protected SQLCursor sqlSelect(String sql) throws SQLException {
    Connection conn = getTargetDatabase().getConnection();
    Statement stmt = conn.createStatement();
    return new SQLCursor(stmt, stmt.executeQuery(sql));
  }

  protected int sqlExec(String sql) throws SQLException {
    Connection conn = getTargetDatabase().getConnection();
    Statement stmt = conn.createStatement();
    int rowsAffected = -1;
    try {
      rowsAffected = stmt.executeUpdate(sql);
    } finally {
      stmt.close();
    }
    return rowsAffected;
  }

  public void setTargetDatabase(JdbcDatabase targetDatabase) {
    this.targetDatabase = targetDatabase;
  }
}
