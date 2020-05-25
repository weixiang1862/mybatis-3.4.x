/**
 * Copyright 2009-2016 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a database connection.
 * Handles the connection lifecycle that comprises: its creation, preparation, commit/rollback and close.
 * <p>
 * MyBatis的Transaction是对JDBC事务、Connection的封装，不仅包含了事务的提交回滚操作，还包含了Connection的回收、close，所以MyBatis的Transaction是对整个query生命周期的封装
 *
 * @author Clinton Begin
 */
public interface Transaction {

    /**
     * Retrieve inner database connection
     * <p>
     * 获取Transaction对应的Connection对象
     *
     * @return DataBase connection
     * @throws SQLException
     */
    Connection getConnection() throws SQLException;

    /**
     * Commit inner database connection.
     * <p>
     * 对应数据库事务的commit
     *
     * @throws SQLException
     */
    void commit() throws SQLException;

    /**
     * Rollback inner database connection.
     * <p>
     * 对应数据库事务的rollback
     *
     * @throws SQLException
     */
    void rollback() throws SQLException;

    /**
     * Close inner database connection.
     * <p>
     * 对应connection的close或者回收
     *
     * @throws SQLException
     */
    void close() throws SQLException;

    /**
     * Get transaction timeout if set
     * <p>
     * timeout并不是Transaction的概念，但是业务上如SpringManagedTransaction实现了事务的timeout
     *
     * @throws SQLException
     */
    Integer getTimeout() throws SQLException;

}
