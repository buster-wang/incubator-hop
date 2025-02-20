/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// CHECKSTYLE:FileLength:OFF
package org.apache.hop.pipeline.transforms.javascript;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileUtil;
import org.apache.hop.core.Const;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopFileException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.ValueDataUtil;
import org.apache.hop.core.util.EnvUtil;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.pipeline.engine.IPipelineEngine;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transforms.loadfileinput.LoadFileInput;
import org.apache.hop.ui.hopgui.HopGui;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.mozilla.javascript.*;

import java.io.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptValuesAddedFunctions extends ScriptableObject {

  public static final long serialVersionUID = 1L;

  public static final int STRING_FUNCTION = 0;
  public static final int NUMERIC_FUNCTION = 1;
  public static final int DATE_FUNCTION = 2;
  public static final int LOGIC_FUNCTION = 3;
  public static final int SPECIAL_FUNCTION = 4;
  public static final int FILE_FUNCTION = 5;

  public static String[] jsFunctionList = {
    "appendToFile",
    "getPipelineName",
    "writeToLog",
    "getFiscalDate",
    "getProcessCount",
    "ceil",
    "floor",
    "abs",
    "getDayNumber",
    "isWorkingDay",
    "fireToDB",
    "getNextWorkingDay",
    "quarter",
    "dateDiff",
    "dateAdd",
    "fillString",
    "isCodepage",
    "ltrim",
    "rtrim",
    "lpad",
    "rpad",
    "week",
    "month",
    "year",
    "str2RegExp",
    "fileExists",
    "touch",
    "isRegExp",
    "date2str",
    "str2date",
    "replace",
    "decode",
    "isNum",
    "isDate",
    "lower",
    "upper",
    "str2num",
    "num2str",
    "Alert",
    "setEnvironmentVar",
    "getEnvironmentVar",
    "LoadScriptFile",
    "LoadScriptFromTab",
    "print",
    "println",
    "resolveIP",
    "trim",
    "substr",
    "getVariable",
    "setVariable",
    "LuhnCheck",
    "getDigitsOnly",
    "indexOf",
    "getOutputRowMeta",
    "getInputRowMeta",
    "createRowCopy",
    "putRow",
    "deleteFile",
    "createFolder",
    "copyFile",
    "getFileSize",
    "isFile",
    "isFolder",
    "getShortFilename",
    "getFileExtension",
    "getParentFoldername",
    "getLastModifiedTime",
    "trunc",
    "truncDate",
    "moveFile",
    "execProcess",
    "isEmpty",
    "isMailValid",
    "escapeXml",
    "removeDigits",
    "initCap",
    "protectXmlCdata",
    "unEscapeXml",
    "escapeSql",
    "escapeHtml",
    "unEscapeHtml",
    "loadFileContent",
    "getOcuranceString",
    "removeCRLF"
  };

  enum VariableScope {
    SYSTEM,
    ROOT,
    PARENT,
    GRAND_PARENT
  }

  // This is only used for reading, so no concurrency problems.
  // todo: move in the real variables of the transform.

  // Functions to Add
  // date2num, num2date,
  // fisc_date, isNull
  //

  public static String getDigitsOnly(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.getDigitsOnly(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call getDigitsOnly requires 1 argument.");
    }
  }

  public static boolean LuhnCheck(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    boolean returnCode = false;

    if (argList.length == 1) {
      if (!isNull(argList) && !isUndefined(argList)) {
        try {
          int sum = 0;
          int digit = 0;
          int addend = 0;
          boolean timesTwo = false;
          String argstring = Context.toString(argList[0]);

          for (int i = argstring.length() - 1; i >= 0; i--) {
            digit = Integer.parseInt(argstring.substring(i, i + 1));
            if (timesTwo) {
              addend = digit * 2;
              if (addend > 9) {
                addend -= 9;
              }
            } else {
              addend = digit;
            }
            sum += addend;
            timesTwo = !timesTwo;
          }

          int modulus = sum % 10;
          if (modulus == 0) {
            returnCode = true;
          }
        } catch (Exception e) {
          // No Need to throw exception
          // This means that input can not be parsed to Integer
        }
      }
    } else {
      throw Context.reportRuntimeError("The function call LuhnCheck requires 1 argument.");
    }
    return returnCode;
  }

  public static int indexOf(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    int returnIndex = -1;

    if (argList.length == 2 || argList.length == 3) {
      if (!isNull(argList) && !isUndefined(argList)) {
        String string = Context.toString(argList[0]);
        String subString = Context.toString(argList[1]);

        int fromIndex = 0;
        if (argList.length == 3) {
          fromIndex = (int) Math.round(Context.toNumber(argList[2]));
        }
        returnIndex = string.indexOf(subString, fromIndex);
      }
    } else {
      throw Context.reportRuntimeError("The function call indexOf requires 2 or 3 arguments");
    }
    return returnIndex;
  }

  public static Object getPipelineName(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      Object objTranName = Context.toString(actualObject.get("_PipelineName_", actualObject));
      return objTranName;
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static void appendToFile(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    if (!isNull(argList) && !isUndefined(argList)) {
      try {
        FileOutputStream file = new FileOutputStream(Context.toString(argList[0]), true);
        DataOutputStream out = new DataOutputStream(file);
        out.writeBytes(Context.toString(argList[1]));
        out.flush();
        out.close();
      } catch (Exception er) {
        throw Context.reportRuntimeError(er.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call appendToFile requires arguments.");
    }
  }

  public static Object getFiscalDate(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    if (argList.length == 2) {
      try {
        if (isNull(argList)) {
          return null;
        } else if (isUndefined(argList)) {
          return Context.getUndefinedValue();
        }
        Date dIn = (Date) Context.jsToJava(argList[0], Date.class);
        Calendar startDate = Calendar.getInstance();
        Calendar fisStartDate = Calendar.getInstance();
        Calendar fisOffsetDate = Calendar.getInstance();
        startDate.setTime(dIn);
        Format dfFormatter = new SimpleDateFormat("dd.MM.yyyy");
        String strOffsetDate =
            Context.toString(argList[1]) + String.valueOf(startDate.get(Calendar.YEAR));
        Date dOffset = (Date) dfFormatter.parseObject(strOffsetDate);
        fisOffsetDate.setTime(dOffset);

        String strFisStartDate = "01.01." + String.valueOf(startDate.get(Calendar.YEAR) + 1);
        fisStartDate.setTime((Date) dfFormatter.parseObject(strFisStartDate));
        int iDaysToAdd =
            (int) ((startDate.getTimeInMillis() - fisOffsetDate.getTimeInMillis()) / 86400000);
        fisStartDate.add(Calendar.DATE, iDaysToAdd);
        return fisStartDate.getTime();
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call getFiscalDate requires 2 arguments.");
    }
  }

  public static double getProcessCount(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    if (argList.length == 1) {
      try {
        Object scmO = actualObject.get("_transform_", actualObject);
        ITransform scm = (ITransform) Context.jsToJava(scmO, ITransform.class);
        String strType = Context.toString(argList[0]).toLowerCase();

        if (strType.equals("i")) {
          return scm.getLinesInput();
        } else if (strType.equals("o")) {
          return scm.getLinesOutput();
        } else if (strType.equals("r")) {
          return scm.getLinesRead();
        } else if (strType.equals("u")) {
          return scm.getLinesUpdated();
        } else if (strType.equals("w")) {
          return scm.getLinesWritten();
        } else if (strType.equals("e")) {
          return scm.getLinesRejected();
        } else {
          return 0;
        }
      } catch (Exception e) {
        return 0;
      }
    } else {
      throw Context.reportRuntimeError("The function call getProcessCount requires 1 argument.");
    }
  }

  public static void writeToLog(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    switch (argList.length) {
      case 1:
        try {
          if (!isNull(argList) && !isUndefined(argList)) {
            Object scmO = actualObject.get("_transform_", actualObject);
            ScriptValues scm = (ScriptValues) Context.jsToJava(scmO, ScriptValues.class);
            String strMessage = Context.toString(argList[0]);
            scm.logDebug(strMessage);
          }
        } catch (Exception e) {
          // Ignore errors
        }
        break;
      case 2:
        try {
          if (!isNull(argList) && !isUndefined(argList)) {
            Object scmO = actualObject.get("_transform_", actualObject);
            ScriptValues scm = (ScriptValues) Context.jsToJava(scmO, ScriptValues.class);

            String strType = Context.toString(argList[0]).toLowerCase();
            String strMessage = Context.toString(argList[1]);
            if (strType.equals("b")) {
              scm.logBasic(strMessage);
            } else if (strType.equals("d")) {
              scm.logDebug(strMessage);
            } else if (strType.equals("l")) {
              scm.logDetailed(strMessage);
            } else if (strType.equals("e")) {
              scm.logError(strMessage);
            } else if (strType.equals("m")) {
              scm.logMinimal(strMessage);
            } else if (strType.equals("r")) {
              scm.logRowlevel(strMessage);
            }
          }
        } catch (Exception e) {
          // Ignore errors
        }
        break;
      default:
        throw Context.reportRuntimeError("The function call writeToLog requires 1 or 2 arguments.");
    }
  }

  private static boolean isUndefined(Object argList) {
    return isUndefined(new Object[] {argList}, new int[] {0});
  }

  private static boolean isUndefined(Object[] argList, int[] iArrToCheck) {
    for (int i = 0; i < iArrToCheck.length; i++) {
      if (argList[iArrToCheck[i]].equals(Context.getUndefinedValue())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNull(Object argList) {
    return isNull(new Object[] {argList}, new int[] {0});
  }

  private static boolean isNull(Object[] argList) {
    for (int i = 0; i < argList.length; i++) {
      if (argList[i] == null || argList[i].equals(null)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNull(Object[] argList, int[] iArrToCheck) {
    for (int i = 0; i < iArrToCheck.length; i++) {
      if (argList[iArrToCheck[i]] == null || argList[iArrToCheck[i]].equals(null)) {
        return true;
      }
    }
    return false;
  }

  public static Object abs(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        } else {
          return Double.valueOf(Math.abs(Context.toNumber(argList[0])));
        }
      } catch (Exception e) {
        return null;
      }
    } else {
      throw Context.reportRuntimeError("The function call abs requires 1 argument.");
    }
  }

  public static Object ceil(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return Double.NaN;
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        } else {
          return Math.ceil(Context.toNumber(argList[0]));
        }
      } catch (Exception e) {
        return null;
      }
    } else {
      throw Context.reportRuntimeError("The function call ceil requires 1 argument.");
    }
  }

  public static Object floor(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        } else {
          return Double.valueOf(Math.floor(Context.toNumber(argList[0])));
        }
      } catch (Exception e) {
        return null;
      }
    } else {
      throw Context.reportRuntimeError("The function call floor requires 1 argument.");
    }
  }

  public static Object getDayNumber(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 2) {
      try {
        if (isNull(argList[0])) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        } else {
          Date dIn = (Date) Context.jsToJava(argList[0], Date.class);
          String strType = Context.toString(argList[1]).toLowerCase();
          Calendar startDate = Calendar.getInstance();
          startDate.setTime(dIn);
          if (strType.equals("y")) {
            return Double.valueOf(startDate.get(Calendar.DAY_OF_YEAR));
          } else if (strType.equals("m")) {
            return Double.valueOf(startDate.get(Calendar.DAY_OF_MONTH));
          } else if (strType.equals("w")) {
            return Double.valueOf(startDate.get(Calendar.DAY_OF_WEEK));
          } else if (strType.equals("wm")) {
            return Double.valueOf(startDate.get(Calendar.DAY_OF_WEEK_IN_MONTH));
          }
          return Double.valueOf(startDate.get(Calendar.DAY_OF_YEAR));
        }
      } catch (Exception e) {
        return null;
      }
    } else {
      throw Context.reportRuntimeError("The function call getDayNumber requires 2 arguments.");
    }
  }

  public static Object isWorkingDay(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        } else {
          Date dIn = (Date) Context.jsToJava(argList[0], Date.class);
          Calendar startDate = Calendar.getInstance();
          startDate.setTime(dIn);
          if (startDate.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY
              && startDate.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            return Boolean.TRUE;
          }
          return Boolean.FALSE;
        }
      } catch (Exception e) {
        return null;
      }
    } else {
      throw Context.reportRuntimeError("The function call isWorkingDay requires 1 argument.");
    }
  }

  @SuppressWarnings("unused")
  public static Object fireToDB(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    Object oRC = new Object();
    if (argList.length == 2) {
      try {
        Object scmO = actualObject.get("_transform_", actualObject);
        ScriptValues scm = (ScriptValues) Context.jsToJava(scmO, ScriptValues.class);
        String strDBName = Context.toString(argList[0]);
        String strSql = Context.toString(argList[1]);
        DatabaseMeta databaseMeta =
            DatabaseMeta.findDatabase(scm.getPipelineMeta().getDatabases(), strDBName);
        if (databaseMeta == null) {
          throw Context.reportRuntimeError("Database connection not found: " + strDBName);
        }

        // TODO: figure out how to set variables on the connection?
        //
        Database db = new Database(scm, Variables.getADefaultVariableSpace(), databaseMeta);
        db.setQueryLimit(0);
        try {
          db.connect();

          ResultSet rs = db.openQuery(strSql);
          ResultSetMetaData resultSetMetaData = rs.getMetaData();
          int columnCount = resultSetMetaData.getColumnCount();
          if (rs != null) {
            List<Object[]> list = new ArrayList<>();
            while (rs.next()) {
              Object[] objRow = new Object[columnCount];
              for (int i = 0; i < columnCount; i++) {
                objRow[i] = rs.getObject(i + 1);
              }
              list.add(objRow);
            }
            Object[][] resultArr = new Object[list.size()][];
            list.toArray(resultArr);
            db.disconnect();
            return resultArr;
          }
        } catch (Exception er) {
          throw Context.reportRuntimeError(er.toString());
        }
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call fireToDB requires 2 arguments.");
    }
    return oRC;
  }

  public static Object dateDiff(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 3) {
      try {
        if (isNull(argList, new int[] {0, 1, 2})) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList, new int[] {0, 1, 2})) {
          return Context.getUndefinedValue();
        } else {
          Date dIn1 = (Date) Context.jsToJava(argList[0], Date.class);
          Date dIn2 = (Date) Context.jsToJava(argList[1], Date.class);
          String strType = Context.toString(argList[2]).toLowerCase();
          int iRC = 0;

          Calendar startDate = Calendar.getInstance();
          Calendar endDate = Calendar.getInstance();
          startDate.setTime(dIn1);
          endDate.setTime(dIn2);

          /*
           * Changed by: Ingo Klose, SHS VIVEON AG, Date: 27.04.2007
           *
           * Calculating time differences using getTimeInMillis() leads to false results when crossing Daylight
           * Savingstime borders. In order to get correct results the time zone offsets have to be added.
           *
           * Fix: 1. calculate correct milli seconds for start and end date 2. replace endDate.getTimeInMillis() with
           * endL and startDate.getTimeInMillis() with startL
           */
          long endL =
              endDate.getTimeInMillis()
                  + endDate.getTimeZone().getOffset(endDate.getTimeInMillis());
          long startL =
              startDate.getTimeInMillis()
                  + startDate.getTimeZone().getOffset(startDate.getTimeInMillis());

          if (strType.equals("y")) {
            return Double.valueOf(endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR));
          } else if (strType.equals("m")) {
            int iMonthsToAdd = (endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR)) * 12;
            return Double.valueOf(
                (endDate.get(Calendar.MONTH) - startDate.get(Calendar.MONTH)) + iMonthsToAdd);
          } else if (strType.equals("d")) {
            return Double.valueOf(((endL - startL) / 86400000));
          } else if (strType.equals("wd")) {
            int iOffset = -1;
            if (endDate.before(startDate)) {
              iOffset = 1;
            }
            while ((iOffset == 1 && endL < startL) || (iOffset == -1 && endL > startL)) {
              int day = endDate.get(Calendar.DAY_OF_WEEK);
              if ((day != Calendar.SATURDAY) && (day != Calendar.SUNDAY)) {
                iRC++;
              }
              endDate.add(Calendar.DATE, iOffset);
              endL =
                  endDate.getTimeInMillis()
                      + endDate.getTimeZone().getOffset(endDate.getTimeInMillis());
            }
            return Double.valueOf(iRC);
          } else if (strType.equals("w")) {
            int iDays = (int) ((endL - startL) / 86400000);
            return Double.valueOf(iDays / 7);
          } else if (strType.equals("ss")) {
            return Double.valueOf(((endL - startL) / 1000));
          } else if (strType.equals("mi")) {
            return Double.valueOf(((endL - startL) / 60000));
          } else if (strType.equals("hh")) {
            return Double.valueOf(((endL - startL) / 3600000));
          } else {
            return Double.valueOf(((endL - startL) / 86400000));
          }
          /*
           * End Bugfix
           */
        }
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call dateDiff requires 3 arguments.");
    }
  }

  public static Object getNextWorkingDay(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }
        Date dIn = (Date) Context.jsToJava(argList[0], Date.class);
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(dIn);
        startDate.add(Calendar.DATE, 1);
        while (startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
            || startDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
          startDate.add(Calendar.DATE, 1);
        }
        return startDate.getTime();
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call getNextWorkingDay requires 1 argument.");
    }
  }

  public static Object dateAdd(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 3) {
      try {
        if (isNull(argList, new int[] {0, 1, 2})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1, 2})) {
          return Context.getUndefinedValue();
        }
        Date dIn = (Date) Context.jsToJava(argList[0], Date.class);
        String strType = Context.toString(argList[1]).toLowerCase();
        int iValue = (int) Context.toNumber(argList[2]);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dIn);
        if (strType.equals("y")) {
          cal.add(Calendar.YEAR, iValue);
        } else if (strType.equals("m")) {
          cal.add(Calendar.MONTH, iValue);
        } else if (strType.equals("d")) {
          cal.add(Calendar.DATE, iValue);
        } else if (strType.equals("w")) {
          cal.add(Calendar.WEEK_OF_YEAR, iValue);
        } else if (strType.equals("wd")) {
          int iOffset = 0;
          while (iOffset < iValue) {
            cal.add(Calendar.DATE, 1);
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if ((day != Calendar.SATURDAY) && (day != Calendar.SUNDAY)) {
              iOffset++;
            }
          }
        } else if (strType.equals("hh")) {
          cal.add(Calendar.HOUR, iValue);
        } else if (strType.equals("mi")) {
          cal.add(Calendar.MINUTE, iValue);
        } else if (strType.equals("ss")) {
          cal.add(Calendar.SECOND, iValue);
        }
        return cal.getTime();
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call dateAdd requires 3 arguments.");
    }
  }

  public static String fillString(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 2) {
      try {
        if (isNull(argList, new int[] {0, 1})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1})) {
          return (String) Context.getUndefinedValue();
        }
        String fillChar = Context.toString(argList[0]);
        int count = (int) Context.toNumber(argList[1]);
        if (fillChar.length() != 1) {
          throw Context.reportRuntimeError("Please provide a valid Char to the fillString");
        } else {
          char[] chars = new char[count];
          while (count > 0) {
            chars[--count] = fillChar.charAt(0);
          }
          return new String(chars);
        }
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call fillString requires 2 arguments.");
    }
  }

  public static Object isCodepage(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    boolean bRC = false;
    if (argList.length == 2) {
      try {
        if (isNull(argList, new int[] {0, 1})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1})) {
          return Context.getUndefinedValue();
        }
        String strValueToCheck = Context.toString(argList[0]);
        String strCodePage = Context.toString(argList[1]);
        byte[] bytearray = strValueToCheck.getBytes();
        CharsetDecoder d = Charset.forName(strCodePage).newDecoder();
        CharBuffer r = d.decode(ByteBuffer.wrap(bytearray));
        r.toString();
        bRC = true;
      } catch (Exception e) {
        bRC = false;
      }
    } else {
      throw Context.reportRuntimeError("The function call isCodepage requires 2 arguments.");
    }
    return Boolean.valueOf(bRC);
  }

  public static String ltrim(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1) {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }
        String strValueToTrim = Context.toString(argList[0]);
        return strValueToTrim.replaceAll("^\\s+", "");
      } else {
        throw Context.reportRuntimeError("The function call ltrim requires 1 argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError("The function call ltrim is not valid : " + e.getMessage());
    }
  }

  public static String rtrim(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1) {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }
        String strValueToTrim = Context.toString(argList[0]);
        return strValueToTrim.replaceAll("\\s+$", "");
      } else {
        throw Context.reportRuntimeError("The function call rtrim requires 1 argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError("The function call rtrim is not valid : " + e.getMessage());
    }
  }

  public static String lpad(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    try {
      if (argList.length == 3) {
        if (isNull(argList, new int[] {0, 1, 2})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1, 2})) {
          return (String) Context.getUndefinedValue();
        }
        String valueToPad = Context.toString(argList[0]);
        String filler = Context.toString(argList[1]);
        int size = (int) Context.toNumber(argList[2]);

        while (valueToPad.length() < size) {
          valueToPad = filler + valueToPad;
        }
        return valueToPad;
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError("The function call lpad requires 3 arguments.");
    }
    return null;
  }

  public static String rpad(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 3) {
        if (isNull(argList, new int[] {0, 1, 2})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1, 2})) {
          return (String) Context.getUndefinedValue();
        }
        String valueToPad = Context.toString(argList[0]);
        String filler = Context.toString(argList[1]);
        int size = (int) Context.toNumber(argList[2]);

        while (valueToPad.length() < size) {
          valueToPad = valueToPad + filler;
        }
        return valueToPad;
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError("The function call rpad requires 3 arguments.");
    }
    return null;
  }

  public static Object year(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1) {
        if (isNull(argList[0])) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }
        Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dArg1);
        return Double.valueOf(cal.get(Calendar.YEAR));
      } else {
        throw Context.reportRuntimeError("The function call year requires 1 argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static Object month(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1) {
        if (isNull(argList[0])) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }
        Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dArg1);
        return Double.valueOf(cal.get(Calendar.MONTH));
      } else {
        throw Context.reportRuntimeError("The function call month requires 1 argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static Object quarter(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1) {
        if (isNull(argList[0])) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }
        Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dArg1);

        // Patch by Ingo Klose: calendar months start at 0 in java.
        int iMonth = cal.get(Calendar.MONTH);
        if (iMonth <= 2) {
          return Double.valueOf(1);
        } else if (iMonth <= 5) {
          return Double.valueOf(2);
        } else if (iMonth <= 8) {
          return Double.valueOf(3);
        } else {
          return Double.valueOf(4);
        }
      } else {
        throw Context.reportRuntimeError("The function call quarter requires 1 argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static Object week(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1) {
        if (isNull(argList[0])) {
          return Double.valueOf(Double.NaN);
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }
        Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
        Calendar cal = Calendar.getInstance();
        cal.setTime(dArg1);
        return Double.valueOf(cal.get(Calendar.WEEK_OF_YEAR));
      } else {
        throw Context.reportRuntimeError("The function call week requires 1 argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static Object str2RegExp(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String[] strArr = null;
    if (argList.length == 2) {
      try {
        if (isNull(argList, new int[] {0, 1})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1})) {
          return Context.getUndefinedValue();
        }
        String strToMatch = Context.toString(argList[0]);
        Pattern p = Pattern.compile(Context.toString(argList[1]));
        Matcher m = p.matcher(strToMatch);
        if (m.matches() && m.groupCount() > 0) {
          strArr = new String[m.groupCount()];
          for (int i = 1; i <= m.groupCount(); i++) {
            strArr[i - 1] = m.group(i);
          }
        }
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call str2RegExp requires 2 arguments.");
    }
    return strArr;
  }

  public static void touch(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        File file = new File(Context.toString(argList[0]));
        boolean success = file.createNewFile();
        if (!success) {
          file.setLastModified(System.currentTimeMillis());
        }
      } else {
        throw Context.reportRuntimeError("The function call touch requires 1 valid argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static Object fileExists(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return null;
        }
        File file = new File(Context.toString(argList[0]));
        return Boolean.valueOf(file.isFile());
      } else {
        throw Context.reportRuntimeError("The function call fileExists requires 1 valid argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static Object str2date(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    Object oRC = new Object();
    String sArg1 = "";
    String sArg2 = "";
    String sArg3 = "";
    String sArg4 = "";
    switch (argList.length) {
      case 0:
        throw Context.reportRuntimeError(
            "Please provide a valid string to the function call str2date.");
      case 1:
        try {
          if (isNull(argList[0])) {
            return null;
          } else if (isUndefined(argList[0])) {
            return Context.getUndefinedValue();
          }
          sArg1 = Context.toString(argList[0]);
          Format dfFormatter = new SimpleDateFormat();
          oRC = dfFormatter.parseObject(sArg1);
        } catch (Exception e) {
          throw Context.reportRuntimeError(
              "Could not apply local format for " + sArg1 + " : " + e.getMessage());
        }
        break;
      case 2:
        try {
          if (isNull(argList, new int[] {0, 1})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1})) {
            return Context.getUndefinedValue();
          }
          sArg1 = Context.toString(argList[0]);
          sArg2 = Context.toString(argList[1]);
          Format dfFormatter = new SimpleDateFormat(sArg2);
          oRC = dfFormatter.parseObject(sArg1);
        } catch (Exception e) {
          throw Context.reportRuntimeError(
              "Could not apply the given format "
                  + sArg2
                  + " on the string for "
                  + sArg1
                  + " : "
                  + e.getMessage());
        }
        break;
      case 3:
        try {
          if (isNull(argList, new int[] {0, 1, 2})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1, 2})) {
            return Context.getUndefinedValue();
          }
          sArg1 = Context.toString(argList[0]);
          Format dfFormatter;
          sArg2 = Context.toString(argList[1]);
          sArg3 = Context.toString(argList[2]);
          if (sArg3.length() == 2) {
            Locale dfLocale = EnvUtil.createLocale(sArg3);
            dfFormatter = new SimpleDateFormat(sArg2, dfLocale);
            oRC = dfFormatter.parseObject(sArg1);
          } else {
            throw Context.reportRuntimeError("Locale " + sArg3 + " is not 2 characters long.");
          }
        } catch (Exception e) {
          throw Context.reportRuntimeError(
              "Could not apply the local format for locale "
                  + sArg3
                  + " with the given format "
                  + sArg2
                  + " on the string for "
                  + sArg1
                  + " : "
                  + e.getMessage());
        }
        break;
      case 4:
        try {
          if (isNull(argList, new int[] {0, 1, 2, 3})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1, 2, 3})) {
            return Context.getUndefinedValue();
          }
          sArg1 = Context.toString(argList[0]);
          DateFormat dfFormatter;
          sArg2 = Context.toString(argList[1]);
          sArg3 = Context.toString(argList[2]);
          sArg4 = Context.toString(argList[3]);

          // If the timezone is not recognized, java will automatically
          // take GMT.
          TimeZone tz = TimeZone.getTimeZone(sArg4);

          if (sArg3.length() == 2) {
            Locale dfLocale = EnvUtil.createLocale(sArg3);
            dfFormatter = new SimpleDateFormat(sArg2, dfLocale);
            dfFormatter.setTimeZone(tz);
            oRC = dfFormatter.parseObject(sArg1);
          } else {
            throw Context.reportRuntimeError("Locale " + sArg3 + " is not 2 characters long.");
          }
        } catch (Exception e) {
          throw Context.reportRuntimeError(
              "Could not apply the local format for locale "
                  + sArg3
                  + " with the given format "
                  + sArg2
                  + " on the string for "
                  + sArg1
                  + " : "
                  + e.getMessage());
        }
        break;
      default:
        throw Context.reportRuntimeError(
            "The function call str2date requires 1, 2, 3, or 4 arguments.");
    }
    return oRC;
  }

  public static Object date2str(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    Object oRC = new Object();
    switch (argList.length) {
      case 0:
        throw Context.reportRuntimeError(
            "Please provide a valid date to the function call date2str.");
      case 1:
        try {
          if (isNull(argList)) {
            return null;
          } else if (isUndefined(argList)) {
            return Context.getUndefinedValue();
          }
          Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
          if (dArg1.equals(null)) {
            return null;
          }
          Format dfFormatter = new SimpleDateFormat();
          oRC = dfFormatter.format(dArg1);
        } catch (Exception e) {
          throw Context.reportRuntimeError("Could not convert to local format.");
        }
        break;
      case 2:
        try {
          if (isNull(argList, new int[] {0, 1})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1})) {
            return Context.getUndefinedValue();
          }
          Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
          String sArg2 = Context.toString(argList[1]);
          Format dfFormatter = new SimpleDateFormat(sArg2);
          oRC = dfFormatter.format(dArg1);
        } catch (Exception e) {
          throw Context.reportRuntimeError("Could not convert to the given format.");
        }
        break;
      case 3:
        try {
          if (isNull(argList, new int[] {0, 1, 2})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1, 2})) {
            return Context.getUndefinedValue();
          }
          Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
          DateFormat dfFormatter;
          String sArg2 = Context.toString(argList[1]);
          String sArg3 = Context.toString(argList[2]);
          if (sArg3.length() == 2) {
            Locale dfLocale = EnvUtil.createLocale(sArg3.toLowerCase());
            dfFormatter = new SimpleDateFormat(sArg2, dfLocale);
            oRC = dfFormatter.format(dArg1);
          } else {
            throw Context.reportRuntimeError("Locale is not 2 characters long.");
          }
        } catch (Exception e) {
          throw Context.reportRuntimeError("Could not convert to the given local format.");
        }
        break;
      case 4:
        try {
          if (isNull(argList, new int[] {0, 1, 2, 3})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1, 2, 3})) {
            return Context.getUndefinedValue();
          }
          Date dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
          DateFormat dfFormatter;
          String sArg2 = Context.toString(argList[1]);
          String sArg3 = Context.toString(argList[2]);
          String sArg4 = Context.toString(argList[3]);

          // If the timezone is not recognized, java will automatically
          // take GMT.
          TimeZone tz = TimeZone.getTimeZone(sArg4);

          if (sArg3.length() == 2) {
            Locale dfLocale = EnvUtil.createLocale(sArg3.toLowerCase());
            dfFormatter = new SimpleDateFormat(sArg2, dfLocale);
            dfFormatter.setTimeZone(tz);
            oRC = dfFormatter.format(dArg1);
          } else {
            throw Context.reportRuntimeError("Locale is not 2 characters long.");
          }
        } catch (Exception e) {
          throw Context.reportRuntimeError("Could not convert to the given local format.");
        }
        break;
      default:
        throw Context.reportRuntimeError(
            "The function call date2str requires 1, 2, 3, or 4 arguments.");
    }
    return oRC;
  }

  public static Object isRegExp(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    if (argList.length >= 2) {
      if (isNull(argList, new int[] {0, 1})) {
        return null;
      } else if (isUndefined(argList, new int[] {0, 1})) {
        return Context.getUndefinedValue();
      }
      String strToMatch = Context.toString(argList[0]);
      for (int i = 1; i < argList.length; i++) {
        Pattern p = Pattern.compile(Context.toString(argList[i]));
        Matcher m = p.matcher(strToMatch);
        if (m.matches()) {
          return Double.valueOf(i);
        }
      }
    }
    return Double.valueOf(-1);
  }

  public static String upper(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC = "";
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }
        sRC = Context.toString(argList[0]);
        sRC = sRC.toUpperCase();
      } catch (Exception e) {
        throw Context.reportRuntimeError(
            "The function call upper is not valid : " + e.getMessage());
      }
    } else {
      throw Context.reportRuntimeError("The function call upper requires 1 argument.");
    }
    return sRC;
  }

  public static String lower(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC = "";
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }
        sRC = Context.toString(argList[0]);
        sRC = sRC.toLowerCase();
      } catch (Exception e) {
        throw Context.reportRuntimeError(
            "The function call lower is not valid : " + e.getMessage());
      }
    } else {
      throw Context.reportRuntimeError("The function call lower requires 1 argument.");
    }
    return sRC;
  }

  // Converts the given Numeric to a JScript String
  public static String num2str(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC = "";
    switch (argList.length) {
      case 0:
        throw Context.reportRuntimeError("The function call num2str requires at least 1 argument.");
      case 1:
        try {
          if (isNull(argList[0])) {
            return null;
          } else if (isUndefined(argList[0])) {
            return (String) Context.getUndefinedValue();
          }
          double sArg1 = Context.toNumber(argList[0]);
          if (Double.isNaN(sArg1)) {
            throw Context.reportRuntimeError("The first Argument must be a Number.");
          }
          DecimalFormat formatter = new DecimalFormat();
          sRC = formatter.format(sArg1);
        } catch (IllegalArgumentException e) {
          throw Context.reportRuntimeError(
              "Could not apply the given format on the number : " + e.getMessage());
        }
        break;
      case 2:
        try {
          if (isNull(argList, new int[] {0, 1})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1})) {
            return (String) Context.getUndefinedValue();
          }
          double sArg1 = Context.toNumber(argList[0]);
          if (Double.isNaN(sArg1)) {
            throw Context.reportRuntimeError("The first Argument must be a Number.");
          }
          String sArg2 = Context.toString(argList[1]);
          DecimalFormat formatter = new DecimalFormat(sArg2);
          sRC = formatter.format(sArg1);
        } catch (IllegalArgumentException e) {
          throw Context.reportRuntimeError(
              "Could not apply the given format on the number : " + e.getMessage());
        }
        break;
      case 3:
        try {
          if (isNull(argList, new int[] {0, 1, 2})) {
            return null;
          } else if (isUndefined(argList, new int[] {0, 1, 2})) {
            return (String) Context.getUndefinedValue();
          }
          double sArg1 = Context.toNumber(argList[0]);
          if (Double.isNaN(sArg1)) {
            throw Context.reportRuntimeError("The first Argument must be a Number.");
          }
          String sArg2 = Context.toString(argList[1]);
          String sArg3 = Context.toString(argList[2]);
          if (sArg3.length() == 2) {
            DecimalFormatSymbols dfs =
                new DecimalFormatSymbols(EnvUtil.createLocale(sArg3.toLowerCase()));
            DecimalFormat formatter = new DecimalFormat(sArg2, dfs);
            sRC = formatter.format(sArg1);
          }
        } catch (Exception e) {
          throw Context.reportRuntimeError(e.toString());
        }
        break;
      default:
        throw Context.reportRuntimeError(
            "The function call num2str requires 1, 2, or 3 arguments.");
    }

    return sRC;
  }

  // Converts the given String to a JScript Numeric
  public static Object str2num(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    double dRC = 0.00;
    switch (argList.length) {
      case 0:
        throw Context.reportRuntimeError("The function call str2num requires at least 1 argument.");
      case 1:
        try {
          if (isNull(argList[0])) {
            return Double.valueOf(Double.NaN);
          } else if (isUndefined(argList[0])) {
            return Context.getUndefinedValue();
          }
          if (argList[0].equals(null)) {
            return null;
          }
          String sArg1 = Context.toString(argList[0]);
          DecimalFormat formatter = new DecimalFormat();
          dRC = (formatter.parse(Const.ltrim(sArg1))).doubleValue();
        } catch (Exception e) {
          throw Context.reportRuntimeError(
              "Could not convert the given String : " + e.getMessage());
        }
        break;
      case 2:
        try {
          if (isNull(argList, new int[] {0, 1})) {
            return Double.valueOf(Double.NaN);
          } else if (isUndefined(argList, new int[] {0, 1})) {
            return Context.getUndefinedValue();
          }
          String sArg1 = Context.toString(argList[0]);
          String sArg2 = Context.toString(argList[1]);
          if (sArg1.equals("null") || sArg2.equals("null")) {
            return null;
          }
          DecimalFormat formatter = new DecimalFormat(sArg2);
          dRC = (formatter.parse(sArg1)).doubleValue();
          return Double.valueOf(dRC);
        } catch (Exception e) {
          throw Context.reportRuntimeError(
              "Could not convert the String with the given format :" + e.getMessage());
        }
      case 3:
        try {
          if (isNull(argList, new int[] {0, 1, 2})) {
            return Double.valueOf(Double.NaN);
          } else if (isUndefined(argList, new int[] {0, 1, 2})) {
            return Context.getUndefinedValue();
          }
          String sArg1 = Context.toString(argList[0]);
          String sArg2 = Context.toString(argList[1]);
          String sArg3 = Context.toString(argList[2]);
          if (sArg3.length() == 2) {
            DecimalFormatSymbols dfs =
                new DecimalFormatSymbols(EnvUtil.createLocale(sArg3.toLowerCase()));
            DecimalFormat formatter = new DecimalFormat(sArg2, dfs);
            dRC = (formatter.parse(sArg1)).doubleValue();
            return Double.valueOf(dRC);
          }
        } catch (Exception e) {
          throw Context.reportRuntimeError(e.getMessage());
        }
        break;
      default:
        throw Context.reportRuntimeError(
            "The function call str2num requires 1, 2, or 3 arguments.");
    }
    return Double.valueOf(dRC);
  }

  public static Object isNum(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }
        double sArg1 = Context.toNumber(argList[0]);
        if (Double.isNaN(sArg1)) {
          return Boolean.FALSE;
        } else {
          return Boolean.TRUE;
        }
      } catch (Exception e) {
        return Boolean.FALSE;
      }
    } else {
      throw Context.reportRuntimeError("The function call isNum requires 1 argument.");
    }
  }

  public static Object isDate(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }
        /* java.util.Date d = (java.util.Date) */
        Context.jsToJava(argList[0], Date.class);
        return Boolean.TRUE;
      } catch (Exception e) {
        return Boolean.FALSE;
      }
    } else {
      throw Context.reportRuntimeError("The function call isDate requires 1 argument.");
    }
  }

  public static Object decode(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length >= 2) {
        if (isNull(argList, new int[] {0, 1})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1})) {
          return Context.getUndefinedValue();
        }
        Object objToCompare = argList[0];
        for (int i = 1; i < argList.length - 1; i = i + 2) {
          if (argList[i].equals(objToCompare)) {
            return argList[i + 1];
          }
        }
        if (argList.length % 2 == 0) {
          return argList[argList.length - 1];
        } else {
          return objToCompare;
        }
      } else {
        throw Context.reportRuntimeError("The function call decode requires more than 1 argument.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError("The function call decode is not valid : " + e.getMessage());
    }
  }

  public static String replace(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length >= 2 && (argList.length - 1) % 2 == 0) {
        if (isNull(argList, new int[] {0, 1})) {
          return null;
        } else if (isUndefined(argList, new int[] {0, 1})) {
          return (String) Context.getUndefinedValue();
        }
        String objForReplace = Context.toString(argList[0]);
        for (int i = 1; i < argList.length - 1; i = i + 2) {
          objForReplace =
              objForReplace.replaceAll(
                  Context.toString(argList[i]), Context.toString(argList[i + 1]));
        }
        return objForReplace;
      } else {
        throw Context.reportRuntimeError(
            "The function call replace is not valid (wrong number of arguments)");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError("Function call replace is not valid : " + e.getMessage());
    }
  }

  // Implementation of the JS AlertBox
  public static String Alert(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    HopGui hopGui = HopGui.getInstance();
    if (argList.length == 1 && hopGui != null) {
      hopGui
          .getDisplay()
          .asyncExec(
              () -> {
                String strMessage = Context.toString(argList[0]);
                MessageBox mb =
                    new MessageBox(hopGui.getShell(), SWT.OK | SWT.CANCEL | SWT.ICON_INFORMATION);
                mb.setMessage(strMessage);
                mb.setText("alert");
                int answer = mb.open();
                if ((answer & SWT.CANCEL) != 0) {
                  throw new RuntimeException("Alert dialog cancelled by user.");
                }
              });
    }

    return "";
  }

  // Setting EnvironmentVar
  public static void setEnvironmentVar(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sArg1 = "";
    String sArg2 = "";
    if (argList.length == 2) {
      try {
        sArg1 = Context.toString(argList[0]);
        sArg2 = Context.toString(argList[1]);
        System.setProperty(sArg1, sArg2);
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
    } else {
      throw Context.reportRuntimeError("The function call setEnvironmentVar requires 2 arguments.");
    }
  }

  // Returning EnvironmentVar
  public static String getEnvironmentVar(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC = "";
    if (argList.length == 1) {
      try {

        String sArg1 = Context.toString(argList[0]);
        // Function getEnvironmentVar() does not work for user defined variables.
        // check if the system property exists, and if it does not, try getting a Hop var instead
        if (System.getProperties().containsValue(sArg1)) {
          sRC = System.getProperty(sArg1, "");
        } else {
          Object scmo = actualObject.get("_transform_", actualObject);
          Object scmO = Context.jsToJava(scmo, ITransform.class);

          if (scmO instanceof ITransform) {
            ITransform scm = (ITransform) Context.jsToJava(scmO, ITransform.class);
            sArg1 = Context.toString(argList[0]);
            sRC = scm.getVariable(sArg1, "");
          } else {
            // running in test mode, return ""
            sRC = "";
          }
        }

      } catch (Exception e) {
        sRC = "";
      }
    } else {
      throw Context.reportRuntimeError("The function call getEnvironmentVar requires 1 argument.");
    }
    return sRC;
  }

  public static String trim(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC = "";
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }
        sRC = Context.toString(argList[0]);
        sRC = Const.trim(sRC);
      } catch (Exception e) {
        throw Context.reportRuntimeError("The function call trim is not valid : " + e.getMessage());
      }
    } else {
      throw Context.reportRuntimeError("The function call trim requires 1 argument.");
    }
    return sRC;
  }

  public static String substr(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC = "";

    if (argList.length == 2) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }
        sRC = Context.toString(argList[0]);
        int from = (int) Math.round(Context.toNumber(argList[1]));
        sRC = sRC.substring(from);
      } catch (Exception e) {
        throw Context.reportRuntimeError(
            "The function call substr is not valid : " + e.getMessage());
      }
    } else if (argList.length == 3) {
      try {
        int to;
        int strLen;

        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }
        sRC = Context.toString(argList[0]);
        int from = (int) Math.round(Context.toNumber(argList[1]));
        int len = (int) Math.round(Context.toNumber(argList[2]));

        if (from < 0) {
          throw Context.reportRuntimeError("start smaller than 0");
        }
        if (len < 0) {
          len = 0; // Make it compatible with Javascript substr
        }

        to = from + len;
        strLen = sRC.length();
        if (to > strLen) {
          to = strLen;
        }
        sRC = sRC.substring(from, to);
      } catch (Exception e) {
        throw Context.reportRuntimeError(
            "The function call substr is not valid : " + e.getMessage());
      }
    } else {
      throw Context.reportRuntimeError("The function call substr requires 2 or 3 arguments.");
    }
    return sRC;
  }

  // Resolve an IP address
  public static String resolveIP(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC;
    if (argList.length == 2) {
      try {
        InetAddress address = InetAddress.getByName(Context.toString(argList[0]));
        if (Context.toString(argList[1]).equals("IP")) {
          sRC = address.getHostName();
        } else {
          sRC = address.getHostAddress();
        }
        if (sRC.equals(Context.toString(argList[0]))) {
          sRC = "-";
        }
      } catch (Exception e) {
        sRC = "-";
      }
    } else {
      throw Context.reportRuntimeError("The function call resolveIP requires 2 arguments.");
    }

    return sRC;
  }

  // Loading additional JS Files inside the JavaScriptCode
  public static void LoadScriptFile(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    for (int i = 0; i < argList.length; i++) { // don't worry about "undefined" arguments
      checkAndLoadJSFile(actualContext, actualObject, Context.toString(argList[i]));
    }
  }

  // Adding the ScriptsItemTab to the actual running Context
  public static void LoadScriptFromTab(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      for (int i = 0; i < argList.length; i++) { // don't worry about "undefined" arguments
        String strToLoad = Context.toString(argList[i]);
        String strScript = actualObject.get(strToLoad, actualObject).toString();
        actualContext.evaluateString(actualObject, strScript, "_" + strToLoad + "_", 0, null);
      }
    } catch (Exception e) {
      // TODO: DON'T EAT EXCEPTION
    }
  }

  // Print
  public static void print(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    for (int i = 0; i < argList.length; i++) { // don't worry about "undefined" arguments
      System.out.print(Context.toString(argList[i]));
    }
  }

  // Prints Line to the actual System.out
  public static void println(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    print(actualContext, actualObject, argList, functionContext);
    System.out.println();
  }

  // Returns the actual ClassName
  @Override
  public String getClassName() {
    return "SciptValuesAddedFunctions";
  }

  // Evaluates the given ScriptFile
  private static void checkAndLoadJSFile(
      Context actualContext, Scriptable evalScope, String fileName) {
    Reader inStream = null;
    try {
      inStream = new InputStreamReader(HopVfs.getInputStream(fileName));
      actualContext.evaluateReader(evalScope, inStream, fileName, 1, null);
    } catch (FileNotFoundException signal) {
      Context.reportError(
          "Unable to open file \"" + fileName + "\" (reason: \"" + signal.getMessage() + "\")");
    } catch (WrappedException signal) {
      Context.reportError(
          "WrappedException while evaluating file \""
              + fileName
              + "\" (reason: \""
              + signal.getMessage()
              + "\")");
    } catch (EvaluatorException signal) {
      Context.reportError(
          "EvaluatorException while evaluating file \""
              + fileName
              + "\" (reason: \""
              + signal.getMessage()
              + "\")");
    } catch (JavaScriptException signal) {
      Context.reportError(
          "JavaScriptException while evaluating file \""
              + fileName
              + "\" (reason: \""
              + signal.getMessage()
              + "\")");
    } catch (IOException signal) {
      Context.reportError(
          "Error while reading file \""
              + fileName
              + "\" (reason: \""
              + signal.getMessage()
              + "\")");
    } catch (HopFileException signal) {
      Context.reportError(
          "Error while reading file \""
              + fileName
              + "\" (reason: \""
              + signal.getMessage()
              + "\")");
    } finally {
      try {
        if (inStream != null) {
          inStream.close();
        }
      } catch (Exception signal) {
        // Ignore
      }
    }
  }

  public static void setVariable(
      Context actualContext,
      Scriptable actualObject,
      Object[] arguments,
      Function functionContext) {

    if (arguments.length != 3) {
      throw Context.reportRuntimeError("The function call setVariable requires 3 arguments.");
    }

    Object transformObject =
        Context.jsToJava(actualObject.get("_transform_", actualObject), ITransform.class);
    if (transformObject instanceof ITransform) {
      ITransform transform = (ITransform) transformObject;
      IPipelineEngine pipeline = transform.getPipeline();
      final String variableName = Context.toString(arguments[0]);
      final String variableValue = Context.toString(arguments[1]);
      final VariableScope variableScope = getVariableScope(Context.toString(arguments[2]));

      // Set variable in transform's scope so that it can be retrieved in the same transform using
      // getVariable
      transform.setVariable(variableName, variableValue);

      switch (variableScope) {
        case PARENT:
          setParentScopeVariable(pipeline, variableName, variableValue);
          break;
        case GRAND_PARENT:
          setGrandParentScopeVariable(pipeline, variableName, variableValue);
          break;
        case ROOT:
          setRootScopeVariable(pipeline, variableName, variableValue);
          break;
        case SYSTEM:
          setSystemScopeVariable(pipeline, variableName, variableValue);
          break;
      }
    }
  }

  static void setRootScopeVariable(
      IPipelineEngine pipeline, String variableName, String variableValue) {

    pipeline.setVariable(variableName, variableValue);

    IVariables parentSpace = pipeline.getParentVariables();
    while (parentSpace != null) {
      parentSpace.setVariable(variableName, variableValue);
      parentSpace = parentSpace.getParentVariables();
    }
  }

  static void setSystemScopeVariable(
      IPipelineEngine pipeline, final String variableName, final String variableValue) {
    System.setProperty(variableName, variableValue);

    // Set also all the way to the root as else we will take
    //  stale values
    setRootScopeVariable(pipeline, variableName, variableValue);
  }

  static void setParentScopeVariable(
      IPipelineEngine pipeline, String variableName, String variableValue) {
    pipeline.setVariable(variableName, variableValue);

    IVariables parentSpace = pipeline.getParentVariables();
    if (parentSpace != null) {
      parentSpace.setVariable(variableName, variableValue);
    }
  }

  static void setGrandParentScopeVariable(
      IPipelineEngine pipeline, String variableName, String variableValue) {
    pipeline.setVariable(variableName, variableValue);

    IVariables parentSpace = pipeline.getParentVariables();
    if (parentSpace != null) {
      parentSpace.setVariable(variableName, variableValue);
      IVariables grandParentSpace = parentSpace.getParentVariables();
      if (grandParentSpace != null) {
        grandParentSpace.setVariable(variableName, variableValue);
      }
    }
  }

  static VariableScope getVariableScope(String codeOfScope) {
    switch (codeOfScope) {
      case "s":
        return VariableScope.SYSTEM;
      case "r":
        return VariableScope.ROOT;
      case "p":
        return VariableScope.PARENT;
      case "g":
        return VariableScope.GRAND_PARENT;
      default:
        throw Context.reportRuntimeError(
            "The argument type of function call "
                + "setVariable should either be \"s\", \"r\", \"p\", or \"g\".");
    }
  }

  // Returning EnvironmentVar
  public static String getVariable(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String sRC = "";
    String sArg1 = "";
    String sArg2 = "";
    if (argList.length == 2) {
      try {
        Object scmo = actualObject.get("_transform_", actualObject);
        Object scmO = Context.jsToJava(scmo, ITransform.class);

        if (scmO instanceof ITransform) {
          ITransform scm = (ITransform) Context.jsToJava(scmO, ITransform.class);

          sArg1 = Context.toString(argList[0]);
          sArg2 = Context.toString(argList[1]);
          return scm.getVariable(sArg1, sArg2);
        } else {
          // running via the Test button in a dialog
          sArg2 = Context.toString(argList[1]);
          return sArg2;
        }
      } catch (Exception e) {
        sRC = "";
      }
    } else {
      throw Context.reportRuntimeError("The function call getVariable requires 2 arguments.");
    }
    return sRC;
  }

  // Return the output row metadata
  public static IRowMeta getOutputRowMeta(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 0) {
      try {
        Object scmO = actualObject.get("_transform_", actualObject);
        try {
          ScriptValues scm = (ScriptValues) Context.jsToJava(scmO, ScriptValues.class);
          return scm.getOutputRowMeta();
        } catch (Exception e) {
          ScriptValuesDummy scm =
              (ScriptValuesDummy) Context.jsToJava(scmO, ScriptValuesDummy.class);
          return scm.getOutputRowMeta();
        }
      } catch (Exception e) {
        throw Context.reportRuntimeError(
            "Unable to get the output row metadata because of an error: "
                + Const.CR
                + e.toString());
      }
    } else {
      throw Context.reportRuntimeError(
          "The function call getOutputRowMeta doesn't require arguments.");
    }
  }

  // Return the input row metadata
  public static IRowMeta getInputRowMeta(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 0) {
      try {
        Object scmO = actualObject.get("_transform_", actualObject);
        try {
          ScriptValues scm = (ScriptValues) Context.jsToJava(scmO, ScriptValues.class);
          return scm.getInputRowMeta();
        } catch (Exception e) {
          ScriptValuesDummy scm =
              (ScriptValuesDummy) Context.jsToJava(scmO, ScriptValuesDummy.class);
          return scm.getInputRowMeta();
        }
      } catch (Exception e) {
        throw Context.reportRuntimeError(
            "Unable to get the input row metadata because of an error: " + Const.CR + e.toString());
      }
    } else {
      throw Context.reportRuntimeError(
          "The function call getInputRowMeta doesn't require arguments.");
    }
  }

  // Return the input row metadata
  public static Object[] createRowCopy(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        int newSize = (int) Math.round(Context.toNumber(argList[0]));

        Object scmO = actualObject.get("row", actualObject);
        Object[] row = (Object[]) Context.jsToJava(scmO, (new Object[] {}).getClass());

        return RowDataUtil.createResizedCopy(row, newSize);
      } catch (Exception e) {
        throw Context.reportRuntimeError("Unable to create a row copy: " + Const.CR + e.toString());
      }
    } else {
      throw Context.reportRuntimeError(
          "The function call createRowCopy requires a single arguments : the new size of the row");
    }
  }

  // put a row out to the next transforms...
  //
  public static void putRow(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        Object[] newRow = (Object[]) Context.jsToJava(argList[0], (new Object[] {}).getClass());

        Object scmO = actualObject.get("_transform_", actualObject);
        try {
          ScriptValues transform = (ScriptValues) Context.jsToJava(scmO, ScriptValues.class);
          transform.putRow(transform.getOutputRowMeta(), newRow);
        } catch (Exception e) {
          ScriptValuesDummy transform =
              (ScriptValuesDummy) Context.jsToJava(scmO, ScriptValuesDummy.class);
          transform.putRow(transform.getOutputRowMeta(), newRow);
        }

      } catch (Exception e) {
        throw Context.reportRuntimeError(
            "Unable to pass the new row to the next transform(s) because of an error: "
                + Const.CR
                + e.toString());
      }
    } else {
      throw Context.reportRuntimeError(
          "The function call putRow requires 1 argument : the output row data (Object[])");
    }
  }

  public static void deleteFile(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {

        FileObject fileObject = null;

        try {
          fileObject = HopVfs.getFileObject(Context.toString(argList[0]));
          if (fileObject.exists()) {
            if (fileObject.getType() == FileType.FILE) {
              if (!fileObject.delete()) {
                Context.reportRuntimeError(
                    "We can not delete file [" + Context.toString(argList[0]) + "]!");
              }
            }

          } else {
            Context.reportRuntimeError(
                "file [" + Context.toString(argList[0]) + "] can not be found!");
          }
        } catch (IOException e) {
          throw Context.reportRuntimeError("The function call deleteFile is not valid.");
        } finally {
          if (fileObject != null) {
            try {
              fileObject.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call deleteFile is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static void createFolder(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        FileObject fileObject = null;

        try {
          fileObject = HopVfs.getFileObject(Context.toString(argList[0]));
          if (!fileObject.exists()) {
            fileObject.createFolder();
          } else {
            Context.reportRuntimeError(
                "folder [" + Context.toString(argList[0]) + "] already exist!");
          }
        } catch (IOException e) {
          throw Context.reportRuntimeError("The function call createFolder is not valid.");
        } finally {
          if (fileObject != null) {
            try {
              fileObject.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call createFolder is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static void copyFile(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    try {
      if (argList.length == 3
          && !isNull(argList[0])
          && !isNull(argList[1])
          && !isUndefined(argList[0])
          && !isUndefined(argList[1])) {
        FileObject fileSource = null, fileDestination = null;

        try {
          // Source file to copy
          fileSource = HopVfs.getFileObject(Context.toString(argList[0]));
          // Destination filename
          fileDestination = HopVfs.getFileObject(Context.toString(argList[1]));
          if (fileSource.exists()) {
            // Source file exists...
            if (fileSource.getType() == FileType.FILE) {
              // Great..source is a file ...
              boolean overwrite = false;
              if (!argList[1].equals(null)) {
                overwrite = Context.toBoolean(argList[2]);
              }
              boolean destinationExists = fileDestination.exists();
              // Let's copy the file...
              if ((destinationExists && overwrite) || !destinationExists) {
                FileUtil.copyContent(fileSource, fileDestination);
              }
            }
          } else {
            Context.reportRuntimeError(
                "file to copy [" + Context.toString(argList[0]) + "] can not be found!");
          }
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call copyFile throw an error : " + e.toString());
        } finally {
          if (fileSource != null) {
            try {
              fileSource.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
          if (fileDestination != null) {
            try {
              fileDestination.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call copyFileis not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static double getFileSize(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return 0;
        }
        FileObject file = null;

        try {
          // Source file
          file = HopVfs.getFileObject(Context.toString(argList[0]));
          long filesize = 0;
          if (file.exists()) {
            if (file.getType().equals(FileType.FILE)) {
              filesize = file.getContent().getSize();
            } else {
              Context.reportRuntimeError("[" + Context.toString(argList[0]) + "] is not a file!");
            }
          } else {
            Context.reportRuntimeError(
                "file [" + Context.toString(argList[0]) + "] can not be found!");
          }
          return filesize;
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call getFileSize throw an error : " + e.toString());
        } finally {
          if (file != null) {
            try {
              file.close();
            } catch (Exception e) {
              // Ignore close errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call getFileSize is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static boolean isFile(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return false;
        }
        FileObject file = null;

        try {
          // Source file
          file = HopVfs.getFileObject(Context.toString(argList[0]));
          boolean isafile = false;
          if (file.exists()) {
            if (file.getType().equals(FileType.FILE)) {
              isafile = true;
            } else {
              Context.reportRuntimeError("[" + Context.toString(argList[0]) + "] is not a file!");
            }
          } else {
            Context.reportRuntimeError(
                "file [" + Context.toString(argList[0]) + "] can not be found!");
          }
          return isafile;
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call is File throw an error : " + e.toString());
        } finally {
          if (file != null) {
            try {
              file.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call isFile is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static boolean isFolder(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return false;
        }
        FileObject file = null;

        try {
          // Source file
          file = HopVfs.getFileObject(Context.toString(argList[0]));
          boolean isafolder = false;
          if (file.exists()) {
            if (file.getType().equals(FileType.FOLDER)) {
              isafolder = true;
            } else {
              Context.reportRuntimeError("[" + Context.toString(argList[0]) + "] is not a folder!");
            }
          } else {
            Context.reportRuntimeError(
                "folder [" + Context.toString(argList[0]) + "] can not be found!");
          }
          return isafolder;
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call isFolder throw an error : " + e.toString());
        } finally {
          if (file != null) {
            try {
              file.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call isFolder is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static String getShortFilename(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return null;
        }
        FileObject file = null;

        try {
          // Source file
          file = HopVfs.getFileObject(Context.toString(argList[0]));
          String filename = null;
          if (file.exists()) {
            filename = file.getName().getBaseName().toString();

          } else {
            Context.reportRuntimeError(
                "file [" + Context.toString(argList[0]) + "] can not be found!");
          }

          return filename;
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call getShortFilename throw an error : " + e.toString());
        } finally {
          if (file != null) {
            try {
              file.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call getShortFilename is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static String getFileExtension(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return null;
        }
        FileObject file = null;

        try {
          // Source file
          file = HopVfs.getFileObject(Context.toString(argList[0]));
          String extension = null;
          if (file.exists()) {
            extension = file.getName().getExtension().toString();

          } else {
            Context.reportRuntimeError(
                "file [" + Context.toString(argList[0]) + "] can not be found!");
          }

          return extension;
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call getFileExtension throw an error : " + e.toString());
        } finally {
          if (file != null) {
            try {
              file.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call getFileExtension is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static String getParentFoldername(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 1 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return null;
        }
        FileObject file = null;

        try {
          // Source file
          file = HopVfs.getFileObject(Context.toString(argList[0]));
          String folderName = null;
          if (file.exists()) {
            folderName = HopVfs.getFilename(file.getParent());

          } else {
            Context.reportRuntimeError(
                "file [" + Context.toString(argList[0]) + "] can not be found!");
          }

          return folderName;
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call getParentFoldername throw an error : " + e.toString());
        } finally {
          if (file != null) {
            try {
              file.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call getParentFoldername is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static String getLastModifiedTime(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      if (argList.length == 2 && !isNull(argList[0]) && !isUndefined(argList[0])) {
        if (argList[0].equals(null)) {
          return null;
        }
        FileObject file = null;

        try {
          // Source file
          file = HopVfs.getFileObject(Context.toString(argList[0]));
          String dateformat = Context.toString(argList[1]);
          if (isNull(dateformat)) {
            dateformat = "yyyy-MM-dd";
          }
          String lastmodifiedtime = null;
          if (file.exists()) {
            Date lastmodifiedtimedate = new Date(file.getContent().getLastModifiedTime());
            DateFormat dateFormat = new SimpleDateFormat(dateformat);
            lastmodifiedtime = dateFormat.format(lastmodifiedtimedate);

          } else {
            Context.reportRuntimeError(
                "file [" + Context.toString(argList[0]) + "] can not be found!");
          }

          return lastmodifiedtime;
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call getLastModifiedTime throw an error : " + e.toString());
        } finally {
          if (file != null) {
            try {
              file.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call getLastModifiedTime is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static Object trunc(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    try {
      // 1 argument: normal truncation of numbers
      //
      if (argList.length == 1) {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return Context.getUndefinedValue();
        }

        // This is the truncation of a number...
        //
        Double dArg1 = (Double) Context.jsToJava(argList[0], Double.class);
        return Double.valueOf(Math.floor(dArg1));

      } else {
        throw Context.reportRuntimeError("The function call trunc requires 1 argument, a number.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  @SuppressWarnings("fallthrough")
  public static Object truncDate(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    // 2 arguments: truncation of dates to a certain precision
    //
    if (argList.length == 2) {
      if (isNull(argList[0])) {
        return null;
      } else if (isUndefined(argList[0])) {
        return Context.getUndefinedValue();
      }

      // This is the truncation of a date...
      // The second argument specifies the level: ms, s, min, hour, day, month, year
      //
      Date dArg1 = null;
      Integer level = null;
      try {
        dArg1 = (Date) Context.jsToJava(argList[0], Date.class);
        level = (Integer) Context.jsToJava(argList[1], Integer.class);
      } catch (Exception e) {
        throw Context.reportRuntimeError(e.toString());
      }
      return truncDate(dArg1, level);
    } else {
      throw Context.reportRuntimeError(
          "The function call truncDate requires 2 arguments: a date and a level (int)");
    }
  }

  @VisibleForTesting
  static Date truncDate(Date dArg1, Integer level) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(dArg1);

    switch (level.intValue()) {
        // MONTHS
      case 5:
        cal.set(Calendar.MONTH, 0);
        // DAYS
      case 4:
        cal.set(Calendar.DAY_OF_MONTH, 1);
        // HOURS
      case 3:
        cal.set(Calendar.HOUR_OF_DAY, 0);
        // MINUTES
      case 2:
        cal.set(Calendar.MINUTE, 0);
        // SECONDS
      case 1:
        cal.set(Calendar.SECOND, 0);
        // MILI-SECONDS
      case 0:
        cal.set(Calendar.MILLISECOND, 0);
        break;
      default:
        throw Context.reportRuntimeError("Argument of TRUNC of date has to be between 0 and 5");
    }
    return cal.getTime();
  }

  public static void moveFile(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {

    try {
      if (argList.length == 3
          && !isNull(argList[0])
          && !isNull(argList[1])
          && !isUndefined(argList[0])
          && !isUndefined(argList[1])) {
        FileObject fileSource = null;
        FileObject fileDestination = null;

        try {
          // Source file to move
          fileSource = HopVfs.getFileObject(Context.toString(argList[0]));
          // Destination filename
          fileDestination = HopVfs.getFileObject(Context.toString(argList[1]));
          if (fileSource.exists()) {
            // Source file exists...
            if (fileSource.getType() == FileType.FILE) {
              // Great..source is a file ...
              boolean overwrite = false;
              if (!argList[1].equals(null)) {
                overwrite = Context.toBoolean(argList[2]);
              }
              boolean destinationExists = fileDestination.exists();
              // Let's move the file...
              if ((destinationExists && overwrite) || !destinationExists) {
                fileSource.moveTo(fileDestination);
              }
            }
          } else {
            Context.reportRuntimeError(
                "file to move [" + Context.toString(argList[0]) + "] can not be found!");
          }
        } catch (IOException e) {
          throw Context.reportRuntimeError(
              "The function call moveFile throw an error : " + e.toString());
        } finally {
          if (fileSource != null) {
            try {
              fileSource.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
          if (fileDestination != null) {
            try {
              fileDestination.close();
            } catch (Exception e) {
              // Ignore errors
            }
          }
        }

      } else {
        throw Context.reportRuntimeError("The function call copyFile is not valid.");
      }
    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
  }

  public static String execProcess(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    String retval = null;
    if (!isNull(argList) && !isUndefined(argList)) {
      Process processrun = null;
      try {

        String ligne = "";
        StringBuilder buffer = new StringBuilder();
        processrun = Runtime.getRuntime().exec(Context.toString(argList[0]));

        // Get process response
        BufferedReader br = new BufferedReader(new InputStreamReader(processrun.getInputStream()));

        // Read response lines
        while ((ligne = br.readLine()) != null) {
          buffer.append(ligne);
        }

        retval = buffer.toString();

      } catch (Exception er) {
        throw Context.reportRuntimeError(er.toString());
      } finally {
        if (processrun != null) {
          processrun.destroy();
        }
      }
    } else {
      throw Context.reportRuntimeError("The function call execProcess is not valid.");
    }
    return retval;
  }

  public static Boolean isEmpty(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        if (isUndefined(argList[0])) {
          throw new Exception(argList[0] + " is  undefined!");
        }
        if (isNull(argList[0])) {
          return Boolean.TRUE;
        }
        if (Context.toString(argList[0]).length() == 0) {
          return Boolean.TRUE;
        } else {
          return Boolean.FALSE;
        }

      } catch (Exception e) {
        throw Context.reportRuntimeError("Error in isEmpty function: " + e.getMessage());
      }
    } else {
      throw Context.reportRuntimeError("The function call isEmpty is not valid");
    }
  }

  public static Boolean isMailValid(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    Boolean isValid;
    if (argList.length == 1) {
      try {
        if (isUndefined(argList[0])) {
          throw new Exception(argList[0] + " is  undefined!");
        }
        if (isNull(argList[0])) {
          return Boolean.FALSE;
        }
        if (Context.toString(argList[0]).length() == 0) {
          return Boolean.FALSE;
        }

        String email = Context.toString(argList[0]);
        if (email.indexOf('@') == -1 || email.indexOf('.') == -1) {
          return Boolean.FALSE;
        }

        isValid = Boolean.TRUE;

      } catch (Exception e) {
        throw Context.reportRuntimeError("Error in isMailValid function: " + e.getMessage());
      }
      return isValid;
    } else {
      throw Context.reportRuntimeError("The function call isMailValid is not valid");
    }
  }

  public static String escapeXml(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.escapeXml(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call escapeXml requires 1 argument.");
    }
  }

  public static String escapeHtml(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.escapeHtml(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call escapeHtml requires 1 argument.");
    }
  }

  public static String unEscapeHtml(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.unEscapeHtml(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call unEscapeHtml requires 1 argument.");
    }
  }

  public static String unEscapeXml(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.unEscapeXml(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call unEscapeXml requires 1 argument.");
    }
  }

  public static String escapeSql(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.escapeSql(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call escapeSql requires 1 argument.");
    }
  }

  public static String protectXmlCdata(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.protectXmlCdata(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call protectXmlCdata requires 1 argument.");
    }
  }

  public static String removeDigits(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return Const.removeDigits(Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call removeDigits requires 1 argument.");
    }
  }

  public static String initCap(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      return ValueDataUtil.initCap(null, Context.toString(argList[0]));
    } else {
      throw Context.reportRuntimeError("The function call initCap requires 1 argument.");
    }
  }

  public static Object loadFileContent(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    Object oRC = new Object();
    try {

      switch (argList.length) {
        case 0:
          throw Context.reportRuntimeError(
              "Please provide a filename to the function call loadFileContent.");
        case 1:
          try {
            if (isNull(argList)) {
              return null;
            } else if (isUndefined(argList)) {
              return Context.getUndefinedValue();
            }
            // Returns file content
            oRC = new String(LoadFileInput.getFileBinaryContent(Context.toString(argList[0])));
          } catch (Exception e) {
            throw Context.reportRuntimeError(
                "The function call loadFileContent throw an error : " + e.toString());
          }
          break;
        case 2:
          try {
            if (argList[0].equals(null)) {
              return null;
            } else if (isUndefined(argList[0])) {
              return Context.getUndefinedValue();
            }
            String encoding = null;
            if (!isUndefined(argList[1]) && !argList[1].equals(null)) {
              encoding = Context.toString(argList[1]);
            }
            // Returns file content
            oRC =
                new String(
                    LoadFileInput.getFileBinaryContent(Context.toString(argList[0])), encoding);
          } catch (Exception e) {
            throw Context.reportRuntimeError(
                "The function call loadFileContent throw an error : " + e.toString());
          }
          break;

        default:
          throw Context.reportRuntimeError(
              "The function call loadFileContentrequires 1 ou 2 arguments.");
      }

    } catch (Exception e) {
      throw Context.reportRuntimeError(e.toString());
    }
    return oRC;
  }

  public static int getOcuranceString(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    int nr = 0;
    if (argList.length == 2) {
      try {
        if (isNull(argList[0])) {
          return 0;
        } else if (isUndefined(argList[0])) {
          return (Integer) Context.getUndefinedValue();
        }
        if (isNull(argList[1])) {
          return 0;
        } else if (isUndefined(argList[1])) {
          return (Integer) Context.getUndefinedValue();
        }
        String string = Context.toString(argList[0]);
        String searchFor = Context.toString(argList[1]);
        nr = Const.getOcuranceString(string, searchFor);
      } catch (Exception e) {
        throw Context.reportRuntimeError("The function call getOcuranceString is not valid");
      }
    } else {
      throw Context.reportRuntimeError("The function call getOcuranceString is not valid");
    }
    return nr;
  }

  public static String removeCRLF(
      Context actualContext, Scriptable actualObject, Object[] argList, Function functionContext) {
    if (argList.length == 1) {
      try {
        if (isNull(argList[0])) {
          return null;
        } else if (isUndefined(argList[0])) {
          return (String) Context.getUndefinedValue();
        }

        return Const.removeCRLF(Context.toString(argList[0]));
      } catch (Exception e) {
        throw Context.reportRuntimeError("The function call removeCRLF is not valid");
      }
    } else {
      throw Context.reportRuntimeError("The function call removeCRLF is not valid");
    }
  }
}
