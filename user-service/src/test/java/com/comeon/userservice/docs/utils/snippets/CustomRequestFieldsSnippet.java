package com.comeon.userservice.docs.utils.snippets;

import org.springframework.http.MediaType;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.payload.AbstractFieldsSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadSubsectionExtractor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CustomRequestFieldsSnippet extends AbstractFieldsSnippet {

    public CustomRequestFieldsSnippet(String type, PayloadSubsectionExtractor<?> subsectionExtractor,
                                      List<FieldDescriptor> descriptors, Map<String, Object> attributes,
                                      boolean ignoreUndocumentedFields) {
        super(type, descriptors, attributes, ignoreUndocumentedFields, subsectionExtractor);
    }

    @Override
    protected MediaType getContentType(Operation operation) {
        return operation.getRequest().getHeaders().getContentType();
    }

    @Override
    protected byte[] getContent(Operation operation) throws IOException {
        return operation.getRequest().getContent();
    }

}
