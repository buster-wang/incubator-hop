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

package org.apache.hop.ui.core.dialog;

import org.apache.hop.core.Const;
import org.apache.hop.core.SqlStatement;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopDatabaseException;
import org.apache.hop.core.logging.ILoggingObject;
import org.apache.hop.core.logging.LoggingObjectType;
import org.apache.hop.core.logging.SimpleLoggingObject;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import java.util.List;

/**
 * Dialog to display the results of an SQL generation operation.
 *
 * @author Matt
 * @since 19-06-2003
 */
public class SqlStatementsDialog extends Dialog {
  private static final Class<?> PKG = SqlStatementsDialog.class; // For Translator

  public static final ILoggingObject loggingObject =
      new SimpleLoggingObject("SQL Statements Dialog", LoggingObjectType.HOP_GUI, null);

  private final List<SqlStatement> stats;

  private TableView wFields;

  private Shell shell;
  private final PropsUi props;

  private Color red;

  private String transformName;

  private IVariables variables;

  public SqlStatementsDialog(
      Shell parent, IVariables variables, int style, List<SqlStatement> stats) {
    super(parent, style);
    this.stats = stats;
    this.props = PropsUi.getInstance();
    this.variables = variables;

    this.transformName = null;
  }

  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    red = display.getSystemColor(SWT.COLOR_RED);

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX);
    props.setLook(shell);
    shell.setImage(GuiResource.getInstance().getImageDatabase());

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "SQLStatementDialog.Title"));

    int margin = props.getMargin();

    // Add the buttons at the bottom
    //
    Button wExec = new Button(shell, SWT.PUSH);
    wExec.setText(BaseMessages.getString(PKG, "SQLStatementDialog.Button.ExecSQL"));
    wExec.addListener(SWT.Selection, e -> exec());
    Button wEdit = new Button(shell, SWT.PUSH);
    wEdit.setText(BaseMessages.getString(PKG, "SQLStatementDialog.Button.EditTransform"));
    wEdit.addListener(SWT.Selection, e -> edit());
    Button wView = new Button(shell, SWT.PUSH);
    wView.setText(BaseMessages.getString(PKG, "SQLStatementDialog.Button.ViewSql"));
    wView.addListener(SWT.Selection, e -> view());
    Button wClose = new Button(shell, SWT.PUSH);
    wClose.setText(BaseMessages.getString(PKG, "System.Button.Close"));
    wClose.addListener(SWT.Selection, e -> close());
    BaseTransformDialog.positionBottomButtons(
        shell, new Button[] {wExec, wEdit, wView, wClose}, margin, null);

    int nrCols = 4;
    int nrRows = stats.size();

    ColumnInfo[] columns = new ColumnInfo[nrCols];
    columns[0] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "SQLStatementDialog.TableCol.TransformName"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false,
            true);
    columns[1] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "SQLStatementDialog.TableCol.Connection"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false,
            true);
    columns[2] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "SQLStatementDialog.TableCol.SQL"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false,
            true);
    columns[3] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "SQLStatementDialog.TableCol.Error"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false,
            true);

    wFields =
        new TableView(
            variables,
            shell,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI,
            columns,
            nrRows,
            true, // read-only
            null,
            props);

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(0, 0);
    fdFields.right = new FormAttachment(100, 0);
    fdFields.bottom = new FormAttachment(wExec, -2 * margin);
    wFields.setLayoutData(fdFields);

    // Add listeners

    getData();

    BaseDialog.defaultShellHandling(shell, c -> exec(), c -> close());

    return transformName;
  }

  public void dispose() {
    props.setScreen(new WindowProperty(shell));
    shell.dispose();
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    for (int i = 0; i < stats.size(); i++) {
      SqlStatement stat = stats.get(i);
      TableItem ti = wFields.table.getItem(i);

      String name = stat.getTransformName();
      DatabaseMeta dbinfo = stat.getDatabase();
      String sql = stat.getSql();
      String error = stat.getError();

      if (name != null) {
        ti.setText(1, name);
      }
      if (dbinfo != null) {
        ti.setText(2, dbinfo.getName());
      }
      if (sql != null) {
        ti.setText(3, sql);
      }
      if (error != null) {
        ti.setText(4, error);
      }

      Color col = ti.getBackground();
      if (stat.hasError()) {
        col = red;
      }
      ti.setBackground(col);
    }
    wFields.setRowNums();
    wFields.optWidth(true);
  }

  private String getSql() {
    StringBuilder sql = new StringBuilder();

    int[] idx = wFields.table.getSelectionIndices();

    // None selected: don't waste users time: select them all!
    if (idx.length == 0) {
      idx = new int[stats.size()];
      for (int i = 0; i < stats.size(); i++) {
        idx[i] = i;
      }
    }

    for (int i = 0; i < idx.length; i++) {
      SqlStatement stat = stats.get(idx[i]);
      DatabaseMeta di = stat.getDatabase();
      if (i > 0) {
        sql.append(
                "-------------------------------------------------------------------------------------------")
            .append(Const.CR);
      }
      sql.append(
          BaseMessages.getString(PKG, "SQLStatementDialog.Log.Transform", stat.getTransformName()));
      sql.append(
          BaseMessages.getString(
              PKG,
              "SQLStatementDialog.Log.Connection",
              (di != null
                  ? di.getName()
                  : BaseMessages.getString(PKG, "SQLStatementDialog.Log.Undefined"))));
      if (stat.hasSql()) {
        sql.append("-- SQL                  : ");
        sql.append(stat.getSql()).append(Const.CR);
      }
      if (stat.hasError()) {
        sql.append(BaseMessages.getString(PKG, "SQLStatementDialog.Log.Error", stat.getError()));
      }
    }

    return sql.toString();
  }

  // View SQL statement:
  private void view() {
    String sql = getSql();
    EnterTextDialog etd =
        new EnterTextDialog(
            shell,
            BaseMessages.getString(PKG, "SQLStatementDialog.ViewSql.Title"),
            BaseMessages.getString(PKG, "SQLStatementDialog.ViewSql.Message"),
            sql,
            true);
    etd.setReadOnly();
    etd.open();
  }

  private void exec() {
    int[] idx = wFields.table.getSelectionIndices();

    // None selected: don't waste users time: select them all!
    if (idx.length == 0) {
      idx = new int[stats.size()];
      for (int i = 0; i < stats.size(); i++) {
        idx[i] = i;
      }
    }

    int errors = 0;
    for (int i = 0; i < idx.length; i++) {
      SqlStatement stat = stats.get(idx[i]);
      if (stat.hasError()) {
        errors++;
      }
    }

    if (errors == 0) {
      for (int i = 0; i < idx.length; i++) {
        SqlStatement stat = stats.get(idx[i]);
        DatabaseMeta databaseMeta = stat.getDatabase();
        if (databaseMeta != null && !stat.hasError()) {
          Database db = new Database(loggingObject, variables, databaseMeta);
          try {
            db.connect();
            try {
              db.execStatements(stat.getSql());
            } catch (HopDatabaseException dbe) {
              errors++;
              new ErrorDialog(
                  shell,
                  BaseMessages.getString(PKG, "SQLStatementDialog.Error.Title"),
                  BaseMessages.getString(
                      PKG, "SQLStatementDialog.Error.CouldNotExec", stat.getSql()),
                  dbe);
            }
          } catch (HopDatabaseException dbe) {
            new ErrorDialog(
                shell,
                BaseMessages.getString(PKG, "SQLStatementDialog.Error.Title"),
                BaseMessages.getString(
                    PKG,
                    "SQLStatementDialog.Error.CouldNotConnect",
                    (databaseMeta == null ? "" : databaseMeta.getName())),
                dbe);
          } finally {
            db.disconnect();
          }
        }
      }
      if (errors == 0) {
        MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_INFORMATION);
        mb.setMessage(
            BaseMessages.getString(
                PKG, "SQLStatementDialog.Success.Message", Integer.toString(idx.length)));
        mb.setText(BaseMessages.getString(PKG, "SQLStatementDialog.Success.Title"));
        mb.open();
      }
    } else {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setMessage(
          BaseMessages.getString(
              PKG, "SQLStatementDialog.Error.Message", Integer.toString(errors)));
      mb.setText(BaseMessages.getString(PKG, "SQLStatementDialog.Error.Title"));
      mb.open();
    }
  }

  private void edit() {
    int idx = wFields.table.getSelectionIndex();
    if (idx >= 0) {
      transformName = wFields.table.getItem(idx).getText(1);
      dispose();
    } else {
      transformName = null;
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setText(BaseMessages.getString(PKG, "TransformFieldsDialog.OriginTransform.Title"));
      mb.setMessage(BaseMessages.getString(PKG, "TransformFieldsDialog.OriginTransform.Message"));
      mb.open();
    }
  }

  private void close() {
    dispose();
  }
}
