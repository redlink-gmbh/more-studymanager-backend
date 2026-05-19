package io.redlink.more.studymanager.core.properties.model;

import io.redlink.more.studymanager.core.validation.ValidationIssue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Type that allows to refer other values via &lt;id&gt;. Before saving those MUST BE replaced with the actual values.
 * The validation will fail if the stored value contains any templates (&lt;id&gt;)
 */
public class StringTemplateValue extends StringValue {


    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("<[^>]+>");

    public StringTemplateValue(String id) {
        super(id);
    }

    @Override
    public String getType() {
        return "STRINGTEMPLATE";
    }

    protected ValidationIssue doValidate(String value) {
        if(value == null) {
            return super.doValidate(value);
        } else {
            Matcher m = TEMPLATE_PATTERN.matcher(value);
            if (m.find()) {
                return ValidationIssue.error(this, "The value MUST NOT contain any templates such as '" + m.group() + "'");
            }
            return super.validate(value);
        }
    }

}
