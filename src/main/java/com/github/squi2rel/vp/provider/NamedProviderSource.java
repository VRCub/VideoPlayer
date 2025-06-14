package com.github.squi2rel.vp.provider;

public record NamedProviderSource(String name) implements IProviderSource {
    @Override
    public void reply(String text) {
    }
}
