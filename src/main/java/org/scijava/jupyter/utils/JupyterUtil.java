/*
 * Copyright 2017 SciJava.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scijava.jupyter.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.scijava.jupyter.kernel.evaluator.ScijavaEvaluator;

/**
 *
 * @author Hadrien Mary
 */
public class JupyterUtil {

    public static String createKernelJSON(String classpath, String logLevel) {
        return createKernelJSON(classpath, logLevel, null);
    }

    public static String createKernelJSON(String classpath, String logLevel, String javaBinaryPath) {
        JSONObject root = new JSONObject();
        root.put("language", ScijavaEvaluator.DEFAULT_LANGUAGE);
        root.put("display_name", "Scijava");

        JSONArray argv = new JSONArray();

        if (javaBinaryPath == null) {
            argv.add(SystemUtil.getJavaBinary());
        } else {
            argv.add(javaBinaryPath);
        }

        argv.add("-classpath");

        String finalClasspath = "";
        finalClasspath += SystemUtil.getImageJClassPaths();
        
        String classPathSeparator = SystemUtil.getClassPathSeparator();
        
        if (classpath != null) {
            if (finalClasspath.length() > 0) {
                finalClasspath += classPathSeparator + classpath;
            } else {
                finalClasspath += classpath;
            }

        }
        argv.add(finalClasspath);

        argv.add("org.scijava.jupyter.kernel.ScijavaKernel");
        argv.add("-verbose");
        argv.add(logLevel);
        argv.add("-connectionFile");
        argv.add("{connection_file}");
        root.put("argv", argv);

        return root.toJSONString();
    }
}
