package com.openspec.router.adapter;

public interface ProtocolAdapter {
    void validate(Object request) throws AdapterValidationException;

    Object requestToInternal(Object request);

    Object responseToExternal(Object response);
}
