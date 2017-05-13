/* 
 * Copyright 2017 Hadrien Mary.
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
package org.scijava.jupyter.kernel.evaluator;

import com.twosigma.beaker.autocomplete.AutocompleteResult;
import com.twosigma.beaker.evaluator.Evaluator;
import com.twosigma.beaker.jvm.object.SimpleEvaluationObject;
import com.twosigma.jupyter.KernelParameters;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.script.AutoCompleter;
import org.scijava.script.AutoCompletionResult;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;
import org.scijava.thread.ThreadService;

/**
 *
 * @author Hadrien Mary
 */
public class ScijavaEvaluator implements Evaluator {

    public static final String DEFAULT_LANGUAGE = "groovy";

    @Parameter
    private LogService log;

    @Parameter
    private transient ScriptService scriptService;

    @Parameter
    private ThreadService threadService;

    @Parameter
    Context context;

    private final Map<String, ScriptEngine> scriptEngines;
    private final Map<String, ScriptLanguage> scriptLanguages;
    private final Map<String, AutoCompleter> completers;
    private String languageName;

    protected String shellId;
    protected String sessionId;

    public ScijavaEvaluator(Context context, String shellId, String sessionId) {
        context.inject(this);

        this.shellId = shellId;
        this.sessionId = sessionId;

        this.scriptEngines = new HashMap<>();
        this.scriptLanguages = new HashMap<>();
        this.completers = new HashMap<>();

        this.languageName = DEFAULT_LANGUAGE;
    }

    @Override
    public void setShellOptions(KernelParameters kp) throws IOException {
        log.debug("Set shell options : " + kp);
    }

    @Override
    public AutocompleteResult autocomplete(String code, int index) {

        // TODO: we need to find a way the language related to the current cell.
        // For now, we are just using the last used language.
        AutoCompleter completer = this.completers.get(this.languageName);
        ScriptEngine scriptEngine = this.scriptEngines.get(this.languageName);

        AutoCompletionResult result = completer.autocomplete(code, index, scriptEngine);

        List<String> matches = (List<String>) result.getMatches();
        int startIndex = (int) result.getStartIndex();

        return new AutocompleteResult(matches, startIndex);
    }

    @Override
    public void killAllThreads() {
        log.debug("Kill All Threads");
        // Ugly and not working :-(
        System.exit(0);
    }

    @Override
    public void startWorker() {
        // Nothing to do
    }

    @Override
    public void evaluate(SimpleEvaluationObject seo, String code) {

        code = this.setLanguage(code);

        Worker worker = new Worker(this.context, this.scriptEngines, this.scriptLanguages);
        worker.setup(seo, code, this.languageName);
        this.threadService.queue(worker);
    }

    @Override
    public void exit() {
        log.debug("Exiting DefaultEvaluator");
        // Ugly and not working :-(
        System.exit(0);
    }

    private void addLanguage(String languageName) {

        if (scriptService.getLanguageByName(languageName) == null) {
            log.error("Script Language for '" + languageName + "' not found.");
            System.exit(1);
        }

        if (!this.scriptLanguages.keySet().contains(languageName)) {

            Bindings bindings = null;
            if (!this.scriptEngines.isEmpty()) {
                String firstLanguage = this.scriptEngines.keySet().iterator().next();
                bindings = this.scriptEngines.get(firstLanguage).getBindings(ScriptContext.ENGINE_SCOPE);
            }

            log.info("Script Language for '" + languageName + "' found.");
            ScriptLanguage scriptLanguage = scriptService.getLanguageByName(languageName);
            this.scriptLanguages.put(languageName, scriptLanguage);

            ScriptEngine engine = this.scriptLanguages.get(languageName).getScriptEngine();
            this.scriptEngines.put(languageName, engine);

            AutoCompleter completer = scriptLanguage.getAutoCompleter();
            this.completers.put(languageName, completer);

            // Not implemented yet
            //engine.setBindings(this.bindings, ScriptContext.ENGINE_SCOPE);
            if (bindings != null) {
                this.initBindings(bindings, engine, scriptLanguage);
            }

            log.debug("Script Language found for '" + languageName + "'");

        }

    }

    private String setLanguage(String code) {
        if (code.startsWith("#!")) {
            this.languageName = code.substring(2, code.indexOf(System.getProperty("line.separator"))).trim();

            // Return the code string without the first line
            code = code.substring(code.indexOf(System.getProperty("line.separator")) + 1);
        }

        this.addLanguage(this.languageName);
        return code;
    }

    private void initBindings(Bindings bindings, ScriptEngine scriptEngine, ScriptLanguage scriptLanguage) {

        Bindings currentBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.keySet().forEach((String key) -> {
            currentBindings.put(key, scriptLanguage.decode(bindings.get(key)));
        });

    }

}
