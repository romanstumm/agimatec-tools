<!-- CODE GENERATED BY "${generator.templateName}" -->
<beanInfos>
<!--
  <validator id="email" java="com.agimatec.utility.validation.routines.EMailValidation"/>
  -->
<#list classes?sort as class><#if class.dtoCurrentlyActive>
  <!-- based on ${class.name} -->
  <bean id="${class.dtoPackageName}.${class.dtoClassName}" impl="${class.dtoPackageName}.${class.dtoClassName}"><#if
  class.findByGetterAnnotation("javax.persistence.Id")??>
<#assign mainKey = class.findByGetterAnnotation("javax.persistence.Id").dtoName>

    <feature key="mainKey">
      <value class="string">${mainKey}</value>
    </feature></#if>
<#list class.dtoFieldAnnotations as each><#if
  each.element.name!=each.dtoName || each.dtoPath??>    <!-- based on ${each.element.name}<#if each.dtoPath??>.${each.dtoPath}</#if> -->
</#if>
<#assign nullable = each.isNullable()>
<#assign uniqueKey = each.isUnique()>
<#if each.getLength()??><#assign length = each.getLength()><#else><#assign length=255></#if><#if
    each.isRelationship()><#assign tag = "relationship"><#else><#assign tag = "property"></#if><#if
    each.isRelationship() || (nullable?? && !nullable && !(mainKey?? && mainKey==each.dtoName)) ||
    (length?? && length!=255) || (uniqueKey?? && uniqueKey) || (each.dtoOneWay) ||
    each.element.getAnnotation("com.agimatec.annotations.Default")??>  
    <${tag} name="${each.dtoName}"<#if tag="relationship"> beanId="<#if each.dtoBeanType??>${each.dtoBeanType}<#else>??</#if>"</#if><#if
    nullable?? && !nullable && !(mainKey?? && mainKey==each.dtoName)> mandatory="true"</#if><#if
    length?? && length!=255> maxLength="${length}"</#if><#if
    (uniqueKey?? && uniqueKey) || each.dtoOneWay || each.element.getAnnotation("com.agimatec.annotations.Default")??>><#else>/></#if><#if
     uniqueKey?? && uniqueKey>

      <feature key="uniqueKey">
        <value class="boolean">${uniqueKey?string}</value>
      </feature></#if><#if each.dtoOneWay>
      <feature key="readonly">
        <value class="boolean">true</value>
      </feature></#if><#if
    each.element.getAnnotation("com.agimatec.annotations.Default")??><#assign
        defannotation=each.element.getAnnotation("com.agimatec.annotations.Default")><feature key="defValue">
        <value class="string">${defannotation.getStringValue("value")}</value>
      </feature></#if><#if
    (uniqueKey?? && uniqueKey) || each.dtoOneWay || each.element.getAnnotation("com.agimatec.annotations.Default")??>

    </${tag}></#if>
</#if></#list>
  </bean>

</#if></#list>
</beanInfos>