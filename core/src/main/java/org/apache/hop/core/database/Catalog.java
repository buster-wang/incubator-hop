/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.core.database;

/**
 * Contains the information that's stored in a single catalog.
 *
 * @author Matt
 * @since 7-apr-2005
 */
public class Catalog {
  private String catalogName;
  private String[] items;

  public Catalog(String catalogName, String[] items) {
    this.catalogName = catalogName;
    this.items = items;
  }

  public Catalog(String catalogName) {
    this(catalogName, new String[] {});
  }

  /** @return Returns the catalogName. */
  public String getCatalogName() {
    return catalogName;
  }

  /** @param catalogName The catalogName to set. */
  public void setCatalogName(String catalogName) {
    this.catalogName = catalogName;
  }

  /** @return Returns the items. */
  public String[] getItems() {
    return items;
  }

  /** @param items The items to set. */
  public void setItems(String[] items) {
    this.items = items;
  }
}
