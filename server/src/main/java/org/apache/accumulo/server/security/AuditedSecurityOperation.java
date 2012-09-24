/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.server.security;

import java.nio.ByteBuffer;
import java.util.Set;

import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.security.AuditLevel;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.SystemPermission;
import org.apache.accumulo.core.security.TablePermission;
import org.apache.accumulo.core.security.thrift.AuthInfo;
import org.apache.accumulo.core.security.thrift.ThriftSecurityException;
import org.apache.accumulo.server.security.handler.Authenticator;
import org.apache.accumulo.server.security.handler.Authorizor;
import org.apache.accumulo.server.security.handler.PermissionHandler;
import org.apache.log4j.Logger;

/**
 * 
 */
public class AuditedSecurityOperation extends SecurityOperation {
  /**
   * @param author
   * @param authent
   * @param pm
   * @param instanceId
   */
  public AuditedSecurityOperation(Authorizor author, Authenticator authent, PermissionHandler pm, String instanceId) {
    super(author, authent, pm, instanceId);
  }

  public static final Logger log = Logger.getLogger(AuditedSecurityOperation.class);
  
  public static synchronized SecurityOperation getInstance(String instanceId) {
    if (instance == null) {
      instance = new AuditedSecurityOperation(getAuthorizor(instanceId), getAuthenticator(instanceId), getPermHandler(instanceId), instanceId);
    }
    return instance;
  }

  private void audit(AuthInfo credentials, ThriftSecurityException ex, String template, Object... args) {
    log.log(AuditLevel.AUDIT, "Error: authenticated operation failed: " + credentials.user + ": " + String.format(template, args));
  }
  
  private void audit(AuthInfo credentials, String template, Object... args) {
    log.log(AuditLevel.AUDIT, "Using credentials " + credentials.user + ": " + String.format(template, args));
  }
  
  /**
   * @param credentials
   * @param user
   * @param password
   * @return
   * @throws ThriftSecurityException
   */
  public boolean authenticateUser(AuthInfo credentials, String user, ByteBuffer password) throws ThriftSecurityException {
    try {
      boolean result = super.authenticateUser(credentials, user, password);
      audit(credentials, result ? "authenticated" : "failed authentication");
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "authenticateUser");
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @return The given user's authorizations
   * @throws ThriftSecurityException
   */
  public Authorizations getUserAuthorizations(AuthInfo credentials, String user) throws ThriftSecurityException {
    try {
      Authorizations result = super.getUserAuthorizations(credentials, user);
      audit(credentials, "got authorizations for %s", user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "getting authorizations for %s", user);
      throw ex;
    }

  }
  
  /**
   * @param credentials
   * @return
   * @throws ThriftSecurityException
   */
  public Authorizations getUserAuthorizations(AuthInfo credentials) throws ThriftSecurityException {
    return getUserAuthorizations(credentials, credentials.user);
  }
  
  /**
   * @param credentials
   * @param user
   * @param authorizations
   * @throws ThriftSecurityException
   */
  public void changeAuthorizations(AuthInfo credentials, String user, Authorizations authorizations) throws ThriftSecurityException {
    try {
      super.changeAuthorizations(credentials, user, authorizations);
      audit(credentials, "changed authorizations for %s to %s", user, authorizations);
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "changing authorizations for %s", user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param bytes
   * @throws ThriftSecurityException
   */
  public void changePassword(AuthInfo credentials, String user, byte[] pass) throws ThriftSecurityException {
    try {
      super.changePassword(credentials, user, pass);
      audit(credentials, "changed password for %s", user);
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "changing password for %s", user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param bytes
   * @param authorizations
   * @throws ThriftSecurityException
   */
  public void createUser(AuthInfo credentials, String user, byte[] pass, Authorizations authorizations) throws ThriftSecurityException {
    try {
      super.createUser(credentials, user, pass, authorizations);
      audit(credentials, "createUser");
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "createUser %s", user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @throws ThriftSecurityException
   */
  public void dropUser(AuthInfo credentials, String user) throws ThriftSecurityException {
    try {
      super.dropUser(credentials, user);
      audit(credentials, "dropUser");
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "dropUser %s", user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param permission
   * @throws ThriftSecurityException
   */
  public void grantSystemPermission(AuthInfo credentials, String user, SystemPermission permission) throws ThriftSecurityException {
    try {
      super.grantSystemPermission(credentials, user, permission);
      audit(credentials, "granted permission %s for %s", permission, user);
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "granting permission %s for %s", permission, user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param table
   * @param permission
   * @throws ThriftSecurityException
   */
  public void grantTablePermission(AuthInfo credentials, String user, String table, TablePermission permission) throws ThriftSecurityException {
    try {
      super.grantTablePermission(credentials, user, table, permission);
      audit(credentials, "granted permission %s on table %s for %s", permission, table, user);
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "granting permission %s on table for %s", permission, table, user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param permission
   * @throws ThriftSecurityException
   */
  public void revokeSystemPermission(AuthInfo credentials, String user, SystemPermission permission) throws ThriftSecurityException {
    try {
      super.revokeSystemPermission(credentials, user, permission);
      audit(credentials, "revoked permission %s for %s", permission, user);
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "revoking permission %s on %s", permission, user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param table
   * @param permission
   * @throws ThriftSecurityException
   */
  public void revokeTablePermission(AuthInfo credentials, String user, String table, TablePermission permission) throws ThriftSecurityException {
    try {
      super.revokeTablePermission(credentials, user, table, permission);
      audit(credentials, "revoked permission %s on table %s for %s", permission, table, user);
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "revoking permission %s on table for %s", permission, table, user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param permission
   * @return
   * @throws ThriftSecurityException
   */
  public boolean hasSystemPermission(AuthInfo credentials, String user, SystemPermission permission) throws ThriftSecurityException {
    try {
      boolean result = super.hasSystemPermission(credentials, user, permission);
      audit(credentials, "checked permission %s on %s", permission, user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "checking permission %s on %s", permission, user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @param user
   * @param table
   * @param permission
   * @return
   * @throws ThriftSecurityException
   */
  public boolean hasTablePermission(AuthInfo credentials, String user, String table, TablePermission permission) throws ThriftSecurityException {
    try {
      boolean result = super.hasTablePermission(credentials, user, table, permission);
      audit(credentials, "checked permission %s on table %s for %s", permission, table, user);
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "checking permission %s on %s", permission, user);
      throw ex;
    }
  }
  
  /**
   * @param credentials
   * @return
   * @throws ThriftSecurityException
   */
  public Set<String> listUsers(AuthInfo credentials) throws ThriftSecurityException {
    try {
      Set<String> result = super.listUsers(credentials);
      audit(credentials, "listUsers");
      return result;
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "listUsers");
      throw ex;
    }
  }
  
  /**
   * @param systemCredentials
   * @param table
   * @throws ThriftSecurityException
   */
  public void deleteTable(AuthInfo credentials, String table) throws ThriftSecurityException {
    try {
      super.deleteTable(credentials, table);
      audit(credentials, "deleted table %s", table);
    } catch (ThriftSecurityException ex) {
      audit(credentials, ex, "deleting table %s", table);
      throw ex;
    }
  }

  @Override
  public void initializeSecurity(AuthInfo credentials, String rootuser, byte[] rootpass) throws AccumuloSecurityException, ThriftSecurityException {
    super.initializeSecurity(credentials, rootuser, rootpass);
    log.info("Initialized root user with username: " + rootuser + " at the request of user " + credentials.user);
  }
}
