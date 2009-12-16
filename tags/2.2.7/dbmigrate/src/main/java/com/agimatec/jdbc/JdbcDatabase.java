package com.agimatec.jdbc;

import java.sql.Connection;
import java.util.Properties;

/**
 * Description: <br/>
 * User: roman.stumm <br/>
 * Date: 04.04.2007 <br/>
 * Time: 12:33:50 <br/>
 * Copyright: Agimatec GmbH
 */
public interface JdbcDatabase {
    Connection getConnection();

    String getDriverClassName();

    void begin();

    boolean isTransaction();

    void rollback();

    void commit();

    Properties getProperties();

    String getConnectionString();

    void close();

    void init(String driver, String url, Properties props);
}
