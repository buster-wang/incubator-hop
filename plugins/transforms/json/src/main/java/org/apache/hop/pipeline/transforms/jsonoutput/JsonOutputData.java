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

package org.apache.hop.pipeline.transforms.jsonoutput;

import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.Writer;
import java.text.*;

/**
 * @author Matt
 * @since 22-jan-2005
 */
public class JsonOutputData extends BaseTransformData implements ITransformData {
  public IRowMeta inputRowMeta;
  public IRowMeta outputRowMeta;
  public int inputRowMetaSize;

  public int nrFields;

  public int[] fieldIndexes;
  public JSONObject jg;
  public JSONArray ja;
  public int nrRow;
  public boolean rowsAreSafe;
  public NumberFormat nf;
  public DecimalFormat df;
  public DecimalFormatSymbols dfs;

  public SimpleDateFormat daf;
  public DateFormatSymbols dafs;

  public DecimalFormat defaultDecimalFormat;
  public DecimalFormatSymbols defaultDecimalFormatSymbols;

  public SimpleDateFormat defaultDateFormat;
  public DateFormatSymbols defaultDateFormatSymbols;

  public boolean outputValue;
  public boolean writeToFile;

  public String realBlocName;
  public int splitnr;
  public Writer writer;
  public int nrRowsInBloc;

  /** */
  public JsonOutputData() {
    super();
    this.ja = new JSONArray();
    this.nrRow = 0;
    this.outputValue = false;
    this.writeToFile = false;
    this.writer = null;
    this.nrRowsInBloc = 0;
  }
}
