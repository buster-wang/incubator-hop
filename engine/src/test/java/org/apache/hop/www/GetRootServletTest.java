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

package org.apache.hop.www;

import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Tests for GetRootServlet class
 *
 * @author Pavel Sakun
 * @see GetRootServlet
 */
public class GetRootServletTest {
  @Test
  public void testDoGetReturn404StatusCode() throws ServletException, IOException {
    GetRootServlet servlet = new GetRootServlet();
    servlet.setJettyMode(true);
    HttpServletRequest request =
        when(mock(HttpServletRequest.class).getRequestURI()).thenReturn("/wrong_path").getMock();
    HttpServletResponse response = mock(HttpServletResponse.class);
    servlet.doGet(request, response);
    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
  }
}
