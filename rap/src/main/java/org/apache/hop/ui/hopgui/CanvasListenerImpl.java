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

package org.apache.hop.ui.hopgui;

import org.apache.commons.io.IOUtils;
import org.eclipse.rap.rwt.SingletonUtil;
import org.eclipse.rap.rwt.scripting.ClientListener;

import java.io.IOException;

public class CanvasListenerImpl extends ClientListener implements ISingletonProvider {

  @Override
  public Object getInstanceInternal() {
    return SingletonUtil.getSessionInstance(CanvasListenerImpl.class);
  }

  protected CanvasListenerImpl() {
    super(getText());
  }

  private static String getText() {
    String canvasScript = null;
    try {
      String themeId = System.getProperty("HOP_WEB_THEME", "dark");
      if ("dark".equalsIgnoreCase(themeId)) {
        canvasScript =
            IOUtils.toString(CanvasListenerImpl.class.getResourceAsStream("canvas-dark.js"));
      } else {
        canvasScript =
            IOUtils.toString(CanvasListenerImpl.class.getResourceAsStream("canvas-light.js"));
      }
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    return canvasScript;
  }
}
