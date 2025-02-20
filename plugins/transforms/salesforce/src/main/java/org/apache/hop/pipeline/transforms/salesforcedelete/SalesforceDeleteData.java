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

package org.apache.hop.pipeline.transforms.salesforcedelete;

import com.sforce.soap.partner.DeleteResult;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transforms.salesforce.SalesforceTransformData;

/*
 * @author Samatar
 * @since 10-06-2007
 */
public class SalesforceDeleteData extends SalesforceTransformData {
  public IRowMeta inputRowMeta;
  public IRowMeta outputRowMeta;

  public DeleteResult[] deleteResult;

  public String[] deleteId;
  public Object[][] outputBuffer;
  public int iBufferPos;

  public int indexOfKeyField;

  public SalesforceDeleteData() {
    super();

    deleteResult = null;
    iBufferPos = 0;
    indexOfKeyField = -1;
  }
}
