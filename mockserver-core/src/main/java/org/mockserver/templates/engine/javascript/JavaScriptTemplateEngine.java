package org.mockserver.templates.engine.javascript;

import org.mockserver.client.serialization.model.DTO;
import org.mockserver.logging.LogFormatter;
import org.mockserver.model.HttpRequest;
import org.mockserver.templates.engine.serializer.HttpTemplateOutputDeserializer;
import org.mockserver.templates.engine.TemplateEngine;
import org.mockserver.templates.engine.model.HttpRequestTemplateObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @author jamesdbloom
 */
public class JavaScriptTemplateEngine implements TemplateEngine {

    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
    private static Logger logger = LoggerFactory.getLogger(JavaScriptTemplateEngine.class);
    private static LogFormatter logFormatter = new LogFormatter(logger);
    private HttpTemplateOutputDeserializer httpTemplateOutputDeserializer = new HttpTemplateOutputDeserializer();

    public <T> T executeTemplate(String template, HttpRequest httpRequest, Class<? extends DTO<T>> dtoClass) {
        try {
            if (engine != null) {
                engine.eval("function handle(request) {" + template + "} function serialise(request) { return JSON.stringify(handle(JSON.parse(request)), null, 2); }");
                // HttpResponse handle(HttpRequest httpRequest) - ES5
                Object stringifiedResponse = ((Invocable) engine).invokeFunction("serialise", new HttpRequestTemplateObject(httpRequest));
                logFormatter.infoLog("Generated output:{}from template:{}for request:{}", stringifiedResponse, template, httpRequest);
                return httpTemplateOutputDeserializer.deserializer((String) stringifiedResponse, dtoClass);
            } else {
                logger.error("JavaScript based templating is only available in a JVM with the \"nashorn\" JavaScript engine, " +
                        "please use a JVM with the \"nashorn\" JavaScript engine, such as Oracle Java 8+", new RuntimeException("\"nashorn\" JavaScript engine not available"));
            }
        } catch (Exception e) {
            logFormatter.errorLog(e, "Exception transforming template:{}for request:{}", template, httpRequest);
        }
        return null;
    }
}