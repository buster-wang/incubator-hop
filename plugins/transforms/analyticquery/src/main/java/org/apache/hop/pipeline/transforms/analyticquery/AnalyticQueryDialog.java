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

package org.apache.hop.pipeline.transforms.analyticquery;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageDialogWithToggle;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import java.util.List;
import java.util.*;

public class AnalyticQueryDialog extends BaseTransformDialog implements ITransformDialog {
  private static final Class<?> PKG = AnalyticQueryDialog.class; // For Translator

  public static final String STRING_SORT_WARNING_PARAMETER = "AnalyticQuerySortWarning";
  private TableView wGroup;

  private TableView wAgg;

  private final AnalyticQueryMeta input;
  private ColumnInfo[] ciKey;
  private ColumnInfo[] ciReturn;

  private final Map<String, Integer> inputFields;

  public AnalyticQueryDialog(
      Shell parent, IVariables variables, Object in, PipelineMeta pipelineMeta, String sname) {
    super(parent, variables, (BaseTransformMeta) in, pipelineMeta, sname);
    input = (AnalyticQueryMeta) in;
    inputFields = new HashMap<>();
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
    props.setLook(shell);
    setShellImage(shell, input);

    ModifyListener lsMod = e -> input.setChanged();
    backupChanged = input.hasChanged();
    // backupAllRows = input.passAllRows();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "AnalyticQueryDialog.Shell.Title"));

    int middle = props.getMiddlePct();
    int margin = props.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "AnalyticQueryDialog.TransformName.Label"));
    props.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    props.setLook(wTransformName);
    wTransformName.addModifyListener(lsMod);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    Label wlGroup = new Label(shell, SWT.NONE);
    wlGroup.setText(BaseMessages.getString(PKG, "AnalyticQueryDialog.Group.Label"));
    props.setLook(wlGroup);
    FormData fdlGroup = new FormData();
    fdlGroup.left = new FormAttachment(0, 0);
    fdlGroup.top = new FormAttachment(wlTransformName, margin);
    wlGroup.setLayoutData(fdlGroup);

    int nrGroupColumns = 1;

    ciKey = new ColumnInfo[nrGroupColumns];
    ciKey[0] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "AnalyticQueryDialog.ColumnInfo.GroupField"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            new String[] {""},
            false);

    wGroup =
        new TableView(
            variables,
            shell,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
            ciKey,
            0,
            lsMod,
            props);

    Button wGet = new Button(shell, SWT.PUSH);
    wGet.setText(BaseMessages.getString(PKG, "AnalyticQueryDialog.GetFields.Button"));
    FormData fdGet = new FormData();
    fdGet.top = new FormAttachment(wlGroup, margin);
    fdGet.right = new FormAttachment(100, 0);
    wGet.setLayoutData(fdGet);

    FormData fdGroup = new FormData();
    fdGroup.left = new FormAttachment(0, 0);
    fdGroup.top = new FormAttachment(wlGroup, margin);
    fdGroup.right = new FormAttachment(wGet, -margin);
    fdGroup.bottom = new FormAttachment(45, 0);
    wGroup.setLayoutData(fdGroup);

    // THE Aggregate fields
    Label wlAgg = new Label(shell, SWT.NONE);
    wlAgg.setText(BaseMessages.getString(PKG, "AnalyticQueryDialog.Aggregates.Label"));
    props.setLook(wlAgg);
    FormData fdlAgg = new FormData();
    fdlAgg.left = new FormAttachment(0, 0);
    fdlAgg.top = new FormAttachment(wGroup, margin);
    wlAgg.setLayoutData(fdlAgg);

    int nrQueryCols = 4;

    ciReturn = new ColumnInfo[nrQueryCols];
    ciReturn[0] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "AnalyticQueryDialog.ColumnInfo.Name"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false);
    ciReturn[1] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "AnalyticQueryDialog.ColumnInfo.Subject"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            new String[] {""},
            false);
    ciReturn[2] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "AnalyticQueryDialog.ColumnInfo.Type"),
            ColumnInfo.COLUMN_TYPE_CCOMBO,
            QueryField.AggregateType.getDescriptions());
    ciReturn[3] =
        new ColumnInfo(
            BaseMessages.getString(PKG, "AnalyticQueryDialog.ColumnInfo.Value"),
            ColumnInfo.COLUMN_TYPE_TEXT,
            false);
    ciReturn[3].setToolTip(
        BaseMessages.getString(PKG, "AnalyticQueryDialog.ColumnInfo.Value.Tooltip"));

    wAgg =
        new TableView(
            variables,
            shell,
            SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL,
            ciReturn,
            0,
            lsMod,
            props);

    Button wGetAgg = new Button(shell, SWT.PUSH);
    wGetAgg.setText(BaseMessages.getString(PKG, "AnalyticQueryDialog.GetLookupFields.Button"));
    FormData fdGetAgg = new FormData();
    fdGetAgg.top = new FormAttachment(wlAgg, margin);
    fdGetAgg.right = new FormAttachment(100, 0);
    wGetAgg.setLayoutData(fdGetAgg);

    //
    // Search the fields in the background

    final Runnable runnable =
        () -> {
          TransformMeta transformMeta = pipelineMeta.findTransform(transformName);
          if (transformMeta != null) {
            try {
              IRowMeta row = pipelineMeta.getPrevTransformFields(variables, transformMeta);

              // Remember these fields...
              for (int i = 0; i < row.size(); i++) {
                inputFields.put(row.getValueMeta(i).getName(), i);
              }
              setComboBoxes();
            } catch (HopException e) {
              logError(BaseMessages.getString(PKG, "System.Dialog.GetFieldsFailed.Message"));
            }
          }
        };
    new Thread(runnable).start();

    // THE BUTTONS
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    FormData fdAgg = new FormData();
    fdAgg.left = new FormAttachment(0, 0);
    fdAgg.top = new FormAttachment(wlAgg, margin);
    fdAgg.right = new FormAttachment(wGetAgg, -margin);
    fdAgg.bottom = new FormAttachment(wOk, -margin);
    wAgg.setLayoutData(fdAgg);

    // Add listeners
    wOk.addListener(SWT.Selection, e -> ok());
    wGet.addListener(SWT.Selection, e -> get());
    wGetAgg.addListener(SWT.Selection, e -> getAgg());
    wCancel.addListener(SWT.Selection, e -> cancel());

    getData();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  protected void setComboBoxes() {
    // Something was changed in the row.
    //
    final Map<String, Integer> fields = new HashMap<>();

    // Add the currentMeta fields...
    fields.putAll(inputFields);

    Set<String> keySet = fields.keySet();
    List<String> entries = new ArrayList<>(keySet);

    String[] fieldNames = entries.toArray(new String[entries.size()]);

    Const.sortStrings(fieldNames);
    ciKey[0].setComboValues(fieldNames);
    ciReturn[1].setComboValues(fieldNames);
  }

  /** Copy information from the meta-data input to the dialog fields. */
  public void getData() {
    for (GroupField groupField : input.getGroupFields()) {
      TableItem tableItem = new TableItem(wGroup.table, SWT.NONE);
      tableItem.setText(1, Const.NVL(groupField.getFieldName(), ""));
    }
    wGroup.optimizeTableView();
    for (QueryField queryField : input.getQueryFields()) {
      TableItem tableItem = new TableItem(wAgg.table, SWT.NONE);
      int column = 1;
      tableItem.setText(column++, Const.NVL(queryField.getAggregateField(), ""));
      tableItem.setText(column++, Const.NVL(queryField.getSubjectField(), ""));
      tableItem.setText(column++, queryField.getAggregateType().getDescription());
      tableItem.setText(column++, Integer.toString(queryField.getValueField()));
    }
    wAgg.optimizeTableView();

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(backupChanged);
    dispose();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    input.getGroupFields().clear();
    input.getQueryFields().clear();

    for (TableItem tableItem : wGroup.getNonEmptyItems()) {
      input.getGroupFields().add(new GroupField(tableItem.getText(1)));
    }
    for (TableItem tableItem : wAgg.getNonEmptyItems()) {
      int column = 1;
      String field = tableItem.getText(column++);
      String subject = tableItem.getText(column++);
      String aggType = tableItem.getText(column++);
      QueryField.AggregateType aggregateType =
          QueryField.AggregateType.findTypeWithDescription(aggType);
      int value = Const.toInt(tableItem.getText(column++), 0);

      input.getQueryFields().add(new QueryField(field, subject, aggregateType, value));
    }

    transformName = wTransformName.getText();

    if ("Y".equalsIgnoreCase(props.getCustomParameter(STRING_SORT_WARNING_PARAMETER, "Y"))) {
      MessageDialogWithToggle md =
          new MessageDialogWithToggle(
              shell,
              BaseMessages.getString(PKG, "AnalyticQueryDialog.GroupByWarningDialog.DialogTitle"),
              BaseMessages.getString(
                      PKG, "AnalyticQueryDialog.GroupByWarningDialog.DialogMessage", Const.CR)
                  + Const.CR,
              SWT.ICON_WARNING,
              new String[] {
                BaseMessages.getString(PKG, "AnalyticQueryDialog.GroupByWarningDialog.Option1")
              },
              BaseMessages.getString(PKG, "AnalyticQueryDialog.GroupByWarningDialog.Option2"),
              "N".equalsIgnoreCase(props.getCustomParameter(STRING_SORT_WARNING_PARAMETER, "Y")));
      md.open();
      props.setCustomParameter(STRING_SORT_WARNING_PARAMETER, md.getToggleState() ? "N" : "Y");
    }

    dispose();
  }

  private void get() {
    try {
      IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
      if (r != null && !r.isEmpty()) {
        BaseTransformDialog.getFieldsFromPrevious(
            r, wGroup, 1, new int[] {1}, new int[] {}, -1, -1, null);
      }
    } catch (HopException ke) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "AnalyticQueryDialog.FailedToGetFields.DialogTitle"),
          BaseMessages.getString(PKG, "AnalyticQueryDialog.FailedToGetFields.DialogMessage"),
          ke);
    }
  }

  private void getAgg() {
    try {
      IRowMeta r = pipelineMeta.getPrevTransformFields(variables, transformName);
      if (r != null && !r.isEmpty()) {
        BaseTransformDialog.getFieldsFromPrevious(
            r, wAgg, 1, new int[] {1, 2}, new int[] {}, -1, -1, null);
      }
    } catch (HopException ke) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "AnalyticQueryDialog.FailedToGetFields.DialogTitle"),
          BaseMessages.getString(PKG, "AnalyticQueryDialog.FailedToGetFields.DialogMessage"),
          ke);
    }
  }
}
