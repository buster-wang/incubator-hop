/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.pipeline.steps.janino;

import org.apache.hop.core.CheckResult;
import org.apache.hop.core.CheckResultInterface;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopStepException;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.core.row.ValueMetaInterface;
import org.apache.hop.core.row.value.ValueMetaFactory;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.VariableSpace;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.step.BaseStepMeta;
import org.apache.hop.pipeline.step.StepDataInterface;
import org.apache.hop.pipeline.step.StepInjectionMetaEntry;
import org.apache.hop.pipeline.step.StepInterface;
import org.apache.hop.pipeline.step.StepMeta;
import org.apache.hop.pipeline.step.StepMetaInterface;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Contains the meta-data for the Formula step: calculates ad-hoc formula's Powered by Pentaho's "libformula"
 * <p>
 * Created on 22-feb-2007
 */

public class JaninoMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = JaninoMeta.class; // for i18n purposes, needed by Translator!!

  /**
   * The formula calculations to be performed
   */
  private JaninoMetaFunction[] formula;

  public JaninoMeta() {
    super(); // allocate BaseStepMeta
  }

  public JaninoMetaFunction[] getFormula() {
    return formula;
  }

  public void setFormula( JaninoMetaFunction[] calcTypes ) {
    this.formula = calcTypes;
  }

  public void allocate( int nrCalcs ) {
    formula = new JaninoMetaFunction[ nrCalcs ];
  }

  public void loadXML( Node stepnode, IMetaStore metaStore ) throws HopXMLException {
    int nrCalcs = XMLHandler.countNodes( stepnode, JaninoMetaFunction.XML_TAG );
    allocate( nrCalcs );
    for ( int i = 0; i < nrCalcs; i++ ) {
      Node calcnode = XMLHandler.getSubNodeByNr( stepnode, JaninoMetaFunction.XML_TAG, i );
      formula[ i ] = new JaninoMetaFunction( calcnode );
    }
  }

  public String getXML() {
    StringBuilder retval = new StringBuilder();

    if ( formula != null ) {
      for ( int i = 0; i < formula.length; i++ ) {
        retval.append( "       " + formula[ i ].getXML() + Const.CR );
      }
    }

    return retval.toString();
  }

  public boolean equals( Object obj ) {
    if ( obj != null && ( obj.getClass().equals( this.getClass() ) ) ) {
      JaninoMeta m = (JaninoMeta) obj;
      return Objects.equals( getXML(), m.getXML() );
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode( formula );
  }

  public Object clone() {
    JaninoMeta retval = (JaninoMeta) super.clone();
    if ( formula != null ) {
      retval.allocate( formula.length );
      for ( int i = 0; i < formula.length; i++ ) {
        //CHECKSTYLE:Indentation:OFF
        retval.getFormula()[ i ] = (JaninoMetaFunction) formula[ i ].clone();
      }
    } else {
      retval.allocate( 0 );
    }
    return retval;
  }

  public void setDefault() {
    formula = new JaninoMetaFunction[ 0 ];
  }

  @Override
  public void getFields( RowMetaInterface row, String name, RowMetaInterface[] info, StepMeta nextStep,
                         VariableSpace space, IMetaStore metaStore ) throws HopStepException {
    for ( int i = 0; i < formula.length; i++ ) {
      JaninoMetaFunction fn = formula[ i ];
      if ( Utils.isEmpty( fn.getReplaceField() ) ) {
        // Not replacing a field.
        if ( !Utils.isEmpty( fn.getFieldName() ) ) {
          // It's a new field!

          try {
            ValueMetaInterface v = ValueMetaFactory.createValueMeta( fn.getFieldName(), fn.getValueType() );
            v.setLength( fn.getValueLength(), fn.getValuePrecision() );
            v.setOrigin( name );
            row.addValueMeta( v );
          } catch ( Exception e ) {
            throw new HopStepException( e );
          }
        }
      } else {
        // Replacing a field
        int index = row.indexOfValue( fn.getReplaceField() );
        if ( index < 0 ) {
          throw new HopStepException( "Unknown field specified to replace with a formula result: ["
            + fn.getReplaceField() + "]" );
        }
        // Change the data type etc.
        //
        ValueMetaInterface v = row.getValueMeta( index ).clone();
        v.setLength( fn.getValueLength(), fn.getValuePrecision() );
        v.setOrigin( name );
        row.setValueMeta( index, v ); // replace it
      }
    }
  }

  /**
   * Checks the settings of this step and puts the findings in a remarks List.
   *
   * @param remarks  The list to put the remarks in @see org.apache.hop.core.CheckResult
   * @param stepMeta The stepMeta to help checking
   * @param prev     The fields coming from the previous step
   * @param input    The input step names
   * @param output   The output step names
   * @param info     The fields that are used as information by the step
   */
  public void check( List<CheckResultInterface> remarks, PipelineMeta pipelineMeta, StepMeta stepMeta,
                     RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
                     IMetaStore metaStore ) {
    CheckResult cr;
    if ( prev == null || prev.size() == 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_WARNING, BaseMessages.getString(
          PKG, "JaninoMeta.CheckResult.ExpectedInputError" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "JaninoMeta.CheckResult.FieldsReceived", "" + prev.size() ), stepMeta );
      remarks.add( cr );
    }

    // See if we have input streams leading to this step!
    if ( input.length > 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "JaninoMeta.CheckResult.ExpectedInputOk" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "JaninoMeta.CheckResult.ExpectedInputError" ), stepMeta );
      remarks.add( cr );
    }
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, PipelineMeta tr,
                                Pipeline pipeline ) {
    return new Janino( stepMeta, stepDataInterface, cnr, tr, pipeline );
  }

  public StepDataInterface getStepData() {
    return new JaninoData();
  }

  public boolean supportsErrorHandling() {
    return true;
  }

  @Override
  public JaninoMetaInjection getStepMetaInjectionInterface() {
    return new JaninoMetaInjection( this );
  }

  public List<StepInjectionMetaEntry> extractStepMetadataEntries() throws HopException {
    return getStepMetaInjectionInterface().extractStepMetadataEntries();
  }

}