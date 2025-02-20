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

package org.apache.hop.workflow.actions.truncatetables;

import org.apache.hop.core.Const;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopDatabaseException;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.EnterSelectionDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.MetaSelectionLine;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.apache.hop.ui.workflow.action.ActionDialog;
import org.apache.hop.ui.workflow.dialog.WorkflowDialog;
import org.apache.hop.workflow.WorkflowMeta;
import org.apache.hop.workflow.action.IAction;
import org.apache.hop.workflow.action.IActionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import java.util.Arrays;

/**
 * This dialog allows you to edit the Truncate Tables action settings. (select the connection and
 * the table to be truncated)
 *
 * @author Samatar
 * @since 22-07-2008
 */
public class ActionTruncateTablesDialog extends ActionDialog implements IActionDialog {
  private static final Class<?> PKG = ActionTruncateTables.class; // For Translator

  private Shell shell;

  private Button wbTable;

  private Text wName;

  private MetaSelectionLine<DatabaseMeta> wConnection;

  private ActionTruncateTables action;

  private boolean changed;

  private Label wlFields;
  private TableView wFields;

  private Button wbdTablename;
  private Button wPrevious;

  public ActionTruncateTablesDialog(
      Shell parent, IAction action, WorkflowMeta workflowMeta, IVariables variables) {
    super(parent, workflowMeta, variables);
    this.action = (ActionTruncateTables) action;
    if (this.action.getName() == null) {
      this.action.setName(BaseMessages.getString(PKG, "ActionTruncateTables.Name.Default"));
    }
  }

  @Override
  public IAction open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MIN | SWT.MAX | SWT.RESIZE);
    props.setLook(shell);
    WorkflowDialog.setShellImage(shell, action);

    ModifyListener lsMod = (ModifyEvent e) -> action.setChanged();
    changed = action.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "ActionTruncateTables.Title"));

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Buttons go at the very bottom
    //
    Button wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wOk.addListener(SWT.Selection, e -> ok());
    Button wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, e -> cancel());
    BaseTransformDialog.positionBottomButtons(shell, new Button[] {wOk, wCancel}, margin, null);

    // Filename line
    Label wlName = new Label(shell, SWT.RIGHT);
    wlName.setText(BaseMessages.getString(PKG, "ActionTruncateTables.Name.Label"));
    props.setLook(wlName);
    FormData fdlName = new FormData();
    fdlName.left = new FormAttachment(0, 0);
    fdlName.right = new FormAttachment(middle, -margin);
    fdlName.top = new FormAttachment(0, margin);
    wlName.setLayoutData(fdlName);
    wName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    props.setLook(wName);
    wName.addModifyListener(lsMod);
    FormData fdName = new FormData();
    fdName.left = new FormAttachment(middle, 0);
    fdName.top = new FormAttachment(0, margin);
    fdName.right = new FormAttachment(100, 0);
    wName.setLayoutData(fdName);

    // Connection line
    wConnection = addConnectionLine(shell, wName, action.getDatabase(), lsMod);

    Label wlPrevious = new Label(shell, SWT.RIGHT);
    wlPrevious.setText(BaseMessages.getString(PKG, "ActionTruncateTables.Previous.Label"));
    props.setLook(wlPrevious);
    FormData fdlPrevious = new FormData();
    fdlPrevious.left = new FormAttachment(0, 0);
    fdlPrevious.top = new FormAttachment(wConnection, margin);
    fdlPrevious.right = new FormAttachment(middle, -margin);
    wlPrevious.setLayoutData(fdlPrevious);
    wPrevious = new Button(shell, SWT.CHECK);
    props.setLook(wPrevious);
    wPrevious.setToolTipText(BaseMessages.getString(PKG, "ActionTruncateTables.Previous.Tooltip"));
    FormData fdPrevious = new FormData();
    fdPrevious.left = new FormAttachment(middle, 0);
    fdPrevious.top = new FormAttachment(wlPrevious, 0, SWT.CENTER);
    fdPrevious.right = new FormAttachment(100, 0);
    wPrevious.setLayoutData(fdPrevious);
    wPrevious.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {

            setPrevious();
            action.setChanged();
          }
        });

    wbTable = new Button(shell, SWT.PUSH | SWT.CENTER);
    props.setLook(wbTable);
    wbTable.setText(BaseMessages.getString(PKG, "ActionTruncateTables.GetTablenamesList.Auto"));
    FormData fdbTable = new FormData();
    fdbTable.left = new FormAttachment(0, margin);
    fdbTable.right = new FormAttachment(100, -margin);
    fdbTable.top = new FormAttachment(wlPrevious, 2 * margin);
    wbTable.setLayoutData(fdbTable);
    wbTable.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            getTableName();
          }
        });

    wlFields = new Label(shell, SWT.NONE);
    wlFields.setText(BaseMessages.getString(PKG, "ActionTruncateTables.Fields.Label"));
    props.setLook(wlFields);
    FormData fdlFields = new FormData();
    fdlFields.left = new FormAttachment(0, 0);
    fdlFields.right = new FormAttachment(middle, -margin);
    fdlFields.top = new FormAttachment(wbTable, 2 * margin);
    wlFields.setLayoutData(fdlFields);

    // Buttons to the right of the screen...
    wbdTablename = new Button(shell, SWT.PUSH | SWT.CENTER);
    props.setLook(wbdTablename);
    wbdTablename.setText(BaseMessages.getString(PKG, "ActionTruncateTables.TableDelete.Button"));
    wbdTablename.setToolTipText(
        BaseMessages.getString(PKG, "ActionTruncateTables.TableDelete.Tooltip"));
    FormData fdbdTablename = new FormData();
    fdbdTablename.right = new FormAttachment(100, 0);
    fdbdTablename.top = new FormAttachment(wlFields, margin);
    wbdTablename.setLayoutData(fdbdTablename);
    wbdTablename.addListener(
        SWT.Selection,
        e -> {
          int[] idx = wFields.getSelectionIndices();
          wFields.remove(idx);
          wFields.removeEmptyRows();
          wFields.setRowNums();
        });

    int nrRows = action.getTableNames() == null ? 1 : action.getTableNames().length;
    final int nrFieldsRows = nrRows;

    ColumnInfo[] columns =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "ActionTruncateTables.Fields.Table.Label"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ActionTruncateTables.Fields.Schema.Label"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false),
        };

    columns[0].setUsingVariables(true);
    columns[0].setToolTip(BaseMessages.getString(PKG, "ActionTruncateTables.Fields.Table.Tooltip"));
    columns[1].setUsingVariables(true);
    columns[1].setToolTip(
        BaseMessages.getString(PKG, "ActionTruncateTables.Fields.Schema.Tooltip"));

    wFields =
        new TableView(
            variables,
            shell,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI,
            columns,
            nrFieldsRows,
            lsMod,
            props);

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment(0, 0);
    fdFields.top = new FormAttachment(wlFields, margin);
    fdFields.right = new FormAttachment(wbdTablename, -margin);
    fdFields.bottom = new FormAttachment(wOk, -2 * margin);
    wFields.setLayoutData(fdFields);

    // Delete files from the list of files...
    wbdTablename.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            int[] idx = wFields.getSelectionIndices();
            wFields.remove(idx);
            wFields.removeEmptyRows();
            wFields.setRowNums();
          }
        });

    getData();
    setPrevious();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return action;
  }

  private void setPrevious() {
    wlFields.setEnabled(!wPrevious.getSelection());
    wFields.setEnabled(!wPrevious.getSelection());
    wbdTablename.setEnabled(!wPrevious.getSelection());
    wbTable.setEnabled(!wPrevious.getSelection());
  }

  public void dispose() {
    props.setScreen(new WindowProperty(shell));
    shell.dispose();
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    wName.setText(Const.nullToEmpty(action.getName()));
    if (action.getDatabase() != null) {
      wConnection.setText(action.getDatabase().getName());
    }

    String[] tableNames = action.getTableNames();
    String[] schemaNames = action.getSchemaNames();

    if (tableNames != null) {
      for (int i = 0; i < tableNames.length; i++) {
        TableItem ti = wFields.table.getItem(i);
        if (tableNames[i] != null) {
          ti.setText(1, tableNames[i]);
        }
        if (schemaNames[i] != null) {
          ti.setText(2, schemaNames[i]);
        }
      }

      wFields.removeEmptyRows();
      wFields.setRowNums();
      wFields.optWidth(true);
    }
    wPrevious.setSelection(action.isArgFromPrevious());

    wName.selectAll();
    wName.setFocus();
  }

  private void cancel() {
    action.setChanged(changed);
    action = null;
    dispose();
  }

  private void ok() {
    if (Utils.isEmpty(wName.getText())) {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setText(BaseMessages.getString(PKG, "System.TransformActionNameMissing.Title"));
      mb.setMessage(BaseMessages.getString(PKG, "System.ActionNameMissing.Msg"));
      mb.open();
      return;
    }
    action.setName(wName.getText());
    action.setDatabase(getWorkflowMeta().findDatabase(wConnection.getText()));
    action.setArgFromPrevious(wPrevious.getSelection());

    int nrItems = wFields.nrNonEmpty();
    int nr = 0;
    for (int i = 0; i < nrItems; i++) {
      String arg = wFields.getNonEmpty(i).getText(1);
      if (arg != null && arg.length() != 0) {
        nr++;
      }
    }
    String[] tables = new String[nr];
    String[] schemas = new String[nr];
    nr = 0;
    for (int i = 0; i < nrItems; i++) {
      String arg = wFields.getNonEmpty(i).getText(1);
      String wild = wFields.getNonEmpty(i).getText(2);
      if (arg != null && arg.length() != 0) {
        tables[nr] = arg;
        schemas[nr] = wild;
        nr++;
      }
    }

    action.setTableNames(tables);
    action.setSchemaNames(schemas);

    dispose();
  }

  private void getTableName() {
    DatabaseMeta databaseMeta = getWorkflowMeta().findDatabase(wConnection.getText());
    if (databaseMeta != null) {
      Database database = new Database(loggingObject, variables, databaseMeta);
      try {
        database.connect();
        String[] tableNames = database.getTablenames();
        Arrays.sort(tableNames);
        EnterSelectionDialog dialog =
            new EnterSelectionDialog(
                shell,
                tableNames,
                BaseMessages.getString(PKG, "ActionTruncateTables.SelectTables.Title"),
                BaseMessages.getString(PKG, "ActionTruncateTables.SelectTables.Message"));
        dialog.setMulti(true);
        dialog.setAvoidQuickSearch();
        if (dialog.open() != null) {
          int[] idx = dialog.getSelectionIndeces();
          for (int i = 0; i < idx.length; i++) {
            TableItem tableItem = new TableItem(wFields.table, SWT.NONE);
            tableItem.setText(1, tableNames[idx[i]]);
          }
        }
      } catch (HopDatabaseException e) {
        new ErrorDialog(
            shell,
            BaseMessages.getString(PKG, "System.Dialog.Error.Title"),
            BaseMessages.getString(PKG, "ActionTruncateTables.ConnectionError.DialogMessage"),
            e);
      } finally {
        database.disconnect();
      }
      wFields.removeEmptyRows();
      wFields.setRowNums();
      wFields.optWidth(true);
    }
  }
}
