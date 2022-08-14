package com.corvid.bes.util;

//import javax.xml.bind.annotation.XmlRootElement;

//@XmlRootElement(name = "resource")
public final class JaxRsResource {

    String method;

    String uri;

    String resourceClass;

    String methodName;

    public JaxRsResource() {
    }

    public JaxRsResource(String resourceClass, String methodName, String method, String uri) {
        this.resourceClass = resourceClass;
        this.methodName = methodName;
        this.method = method;
        this.uri = uri;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getResourceClass() {
        return resourceClass;
    }

    public void setResourceClass(String resourceClass) {
        this.resourceClass = resourceClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JaxRsResource)) return false;

        JaxRsResource that = (JaxRsResource) o;

        if (!method.equals(that.method)) return false;
        if (!methodName.equals(that.methodName)) return false;
        if (!resourceClass.equals(that.resourceClass)) return false;
        if (!uri.equals(that.uri)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + uri.hashCode();
        result = 31 * result + resourceClass.hashCode();
        result = 31 * result + methodName.hashCode();
        return result;
    }
}