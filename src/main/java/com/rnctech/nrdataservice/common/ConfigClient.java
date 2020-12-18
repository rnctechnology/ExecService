package com.rnctech.nrdataservice.common;

import java.io.InputStream;
import java.util.*;

public interface ConfigClient extends AutoCloseable {
    String decrypt(String cipher);

    String decrypt(String cipher, String salt);

/*    Map<String, Set<String>> getEntitiesAsJava(String[] categories );

    java.util.Map<String, Set<String>> getEntitiesAsJava();

    default java.util.Map<String, Set<String>> getExtractionEntitiesAsJava() {return Collections.emptyMap();}

    String getJobProperty(String propertyName);

    default Map<String, String> getAllJobProperties() {return Collections.emptyMap();}

    String getTenantProperty(String propertyName);

    default Map<String, String> getAllTenantProperties() {return Collections.emptyMap();}

    Map<String, String> getEntityPropertiesAsJava(String instanceName, String entityName);

    Map<String, String> getSourceInstancePropertiesAsJava(String instanceName);

    Map<String, String> getPrimaryKeysAsJava(String instanceName,String entityName);

    Map<String, String> getSourcePropertiesAsJava(String sourceName);

    void updateEntityProperties(String instanceName, Map<String, Map<String, String>> properties );

    void updateEntityProperties(String instanceName, String entity, Map<String, String> properties );

    void updateSourceEntityProperties(String instanceName, Map<String, String> properties );

    void updateEntityLET(String instanceName, String entity, Date date);

    void updateEntityLET(String instanceName, Collection<String> entities, Date date);

    void updateJobProperties(Map<String, String> properties);

    void updateTenantProperties(Map<String, String> properties);

    default void updateFailures(String instance, String entity) {
        updateFailures(instance, Collections.singleton(entity));
    }

    void updateFailures(String instance, Collection<String> entities);

    Map<String, String> getSourceSystemsAsJava();

    String getPasswordSalt();

    String getJobStatus();

    String getLoadType();

    default String getJobType() {return "";}

    default String getJobName() {return "";}

    default String getCategories() {return "";}

    void updateEntityFilterProperties(String instance, String entity, Map<String, Map<String, String>> attributeProperties);

    Map<String, Map<String, Map<String, String>>> getEntityFilterProperties(String instance, String entity, boolean excludeDisabled);
    
    Map<String, Map<String, Map<String, Object>>> getOverriddenAttributes(String sourceName);

    java.util.Map<String, Set<String>> setRunningStatus(java.util.Map<String, Set<String>> entities);

    void setReadyStatus(java.util.Map<String, Set<String>> entities);

    void updateEntitySchema(String sourceInstance, String entityName, Object data);

    default void attachJobFile(String fileName, String fileInfo, InputStream data) {}

    default String getTemplateFile(String templateName) {return null;}

    default void uploadTemplateFile(String fileName, String fileInfo, InputStream data) {}
    
    default byte[] getModel(String context, String modelname, String fname) {return null;}
    
    default void uploadModel(String context, String modelname, String fname, String finfo, InputStream data) {}
    
    default Map<String, String> getSysColumnsAsJava(){return Collections.emptyMap();};

    default Map<String, Map<String, String>> getEntitiesPropertiesAsJava(String instance , Set<String> entities) {return Collections.emptyMap();}

    java.util.Map<String, Set<String>> getJobsOnNodes(String statusTypes, String opSqlType);

    String retrieveJobStatus();

    default String getInstanceFileResource(String ssiName, String resourceName) {return null;}

    default String getEntityFileResource(String ssiName, String entityName, String resourceName) {return null;}

    default String getTemplateFileBySSI(String ss, String instance, String templateName) {return null;}

    default Long getJobId() {return -1l;}*/
}
