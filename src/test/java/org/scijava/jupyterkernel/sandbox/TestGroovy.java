/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scijava.jupyterkernel.sandbox;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import net.imagej.ImageJ;
import net.imagej.Main;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;

/**
 *
 * @author Hadrien Mary
 */
public class TestGroovy {

    public static void main(String[] args) throws ScriptException {
        ImageJ ij = Main.launch(args);

        String code = "";
        //code += "@Grab('org.springframework:spring-orm:3.2.5.RELEASE')\n";
        code += "import org.springframework.jdbc.core.JdbcTemplate\nprintln JdbcTemplate\n";
        code += "println 'test'";
        final Reader input = new StringReader(code);

        ScriptInfo info = new ScriptInfo(ij.context(), "dummy.py", input);
        final String path = info.getPath();

        List<? extends PreprocessorPlugin> pre = ij.plugin().createInstancesOfType(PreprocessorPlugin.class);

        ScriptLanguage scriptLanguage = ij.script().getLanguageByName("groovy");
        ScriptEngine scriptEngine = scriptLanguage.getScriptEngine();

        ScriptModule module;
        module = new ScriptModule(info);
        ij.context().inject(module);
        module.setLanguage(scriptLanguage);

        pre.forEach((p) -> {
            p.process(module);
        });
        
        for (final ModuleItem<?> item : info.inputs()) {
            final String name = item.getName();
            scriptEngine.put(name, module.getInput(name));
        }
        
        scriptEngine.eval(code);
        
        ij.context().dispose();
        
    }

}
