package io.redlink.more.studymanager.core.properties.model;

import io.redlink.more.studymanager.core.validation.ValidationIssue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Type that allows to refer other values via &lt;id&gt;. Before saving those MUST BE replaced with the actual values.
 * The validation will fail if the stored value contains any templates (&lt;id&gt;)
 */
public class StringTemplateValue extends StringValue {


    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("<[(^>)]+>");
    private Set<String> allowList;

    public StringTemplateValue(String id) {
        this(id, null);
    }
    public StringTemplateValue(String id, Set<String> allowList) {
        super(id);
        this.allowList = allowList;
    }

    @Override
    public String getType() {
        return "STRINGTEMPLATE";
    }

    protected ValidationIssue doValidate(String value) {
        if(value == null) {
            return super.doValidate(value);
        }
        if(allowList != null) { //check that only allowed templates are used
            int start = 0;
            Matcher m = TEMPLATE_PATTERN.matcher(value);
            Set<String> notAllowed = new HashSet<>();
            while (m.find(start)) {
                String template = m.group(1);
                if(!allowList.contains(template)) {
                    notAllowed.add(template);
                }
            }
            if (!notAllowed.isEmpty()) {
                return ValidationIssue.error(this, String.format(
                        "The value contains the unknown templates %s (allowed are: %s)!", notAllowed, allowList));
            }
        }
        return super.validate(value);
    }

}
