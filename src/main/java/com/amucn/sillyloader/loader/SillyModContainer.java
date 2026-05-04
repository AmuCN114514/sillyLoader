package com.amucn.sillyloader.loader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SillyModContainer {
    private final Path jarPath;
    private final List<Class<?>> modClasses;
    private final ClassLoader classLoader;
    private final List<Object> instances = new ArrayList<>();
    
    public SillyModContainer(Path jarPath, List<Class<?>> modClasses, ClassLoader classLoader) {
        this.jarPath = jarPath;
        this.modClasses = modClasses;
        this.classLoader = classLoader;
    }
    
    public Path getJarPath() {
        return jarPath;
    }
    
    public List<Class<?>> getModClasses() {
        return modClasses;
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public List<Object> getInstances() {
        return instances;
    }
    
    public void addInstance(Object instance) {
        this.instances.add(instance);
    }
}
