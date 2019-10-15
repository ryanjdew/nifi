/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.marklogic.processor;

import com.marklogic.client.admin.ExtensionMetadata;
import com.marklogic.client.admin.ResourceExtensionsManager;
import com.marklogic.client.io.StringHandle;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExtensionCallMarkLogicIT extends AbstractMarkLogicIT {
    private ResourceExtensionsManager resourceExtensionsManager = null;
    private boolean resourcesLoaded = false;
    @BeforeEach
    public void setup() {
        super.setup();
        if (!resourcesLoaded) {
            loadResourceExtensions();
            resourcesLoaded = true;
        }
    }

    private void loadResourceExtensions() {
        if (resourceExtensionsManager == null) {
            resourceExtensionsManager = getDatabaseClient().newServerConfigManager().newResourceExtensionsManager();
        }
        ExtensionMetadata goodMetadata = new ExtensionMetadata();
        goodMetadata.setScriptLanguage(ExtensionMetadata.JAVASCRIPT);
        goodMetadata.setTitle("NiFi Test Extension - Good");
        goodMetadata.setDescription("This is a test extension that doesn't throw an error");
        goodMetadata.setProvider("MarkLogic");
        goodMetadata.setVersion("0.1");
        StringHandle goodHandle = new StringHandle().with("function get(context, params) {\n" +
                "  return {\"hello\": \"world\"};\n" +
                "};\n" +
                "exports.GET = get;");
        resourceExtensionsManager.writeServices("niFiTestGoodExtension", goodHandle, goodMetadata);

        ExtensionMetadata badMetadata = new ExtensionMetadata();
        badMetadata.setScriptLanguage(ExtensionMetadata.JAVASCRIPT);
        badMetadata.setTitle("NiFi Test Extension - Bad");
        badMetadata.setDescription("This is a test extension that throws an error");
        badMetadata.setProvider("MarkLogic");
        badMetadata.setVersion("0.1");
        StringHandle badHandle = new StringHandle().with("function get(context, params) {\n" +
                "  fn.error(null, 'RESTAPI-SRVEXERR', \n" +
                "           xdmp.arrayValues([500, 'I BLEW UP', 'Not good!']));\n" +
                "};\n" +
                "exports.GET = get;");
        resourceExtensionsManager.writeServices("niFiTestBadExtension", badHandle, badMetadata);
    }

    @Test
    public void testGoodExtension() throws InitializationException {
        TestRunner runner = getNewTestRunner(ExtensionCallMarkLogic.class);
        runner.setProperty(ExtensionCallMarkLogic.EXTENSION_NAME, "niFiTestGoodExtension");
        runner.setProperty(ExtensionCallMarkLogic.REQUIRES_INPUT, "true");
        runner.setProperty(ExtensionCallMarkLogic.PAYLOAD_SOURCE, ExtensionCallMarkLogic.PayloadSources.NONE);
        runner.setProperty(ExtensionCallMarkLogic.METHOD_TYPE, ExtensionCallMarkLogic.MethodTypes.GET);
        addDatabaseClientService(runner);
        runner.assertValid();
        runner.enqueue("test doc");
        runner.run(1);
        runner.assertTransferCount(QueryMarkLogic.SUCCESS, 1);
        runner.shutdown();
    }

    @Test
    public void testBadExtension() throws InitializationException {
        TestRunner runner = getNewTestRunner(ExtensionCallMarkLogic.class);
        runner.setProperty(ExtensionCallMarkLogic.EXTENSION_NAME, "niFiTestBadExtension");
        runner.setProperty(ExtensionCallMarkLogic.REQUIRES_INPUT, "true");
        runner.setProperty(ExtensionCallMarkLogic.PAYLOAD_SOURCE, ExtensionCallMarkLogic.PayloadSources.NONE);
        runner.setProperty(ExtensionCallMarkLogic.METHOD_TYPE, ExtensionCallMarkLogic.MethodTypes.GET);
        addDatabaseClientService(runner);
        runner.assertValid();
        runner.enqueue("test doc");
        runner.run(1);
        runner.assertTransferCount(QueryMarkLogic.FAILURE, 1);
        runner.shutdown();
    }
}
