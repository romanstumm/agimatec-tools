<#list classes?sort as class><#if class.dtoCurrentlyActive>
${generator.outputToFile("${class.dtoPackagePath}/${class.dtoClassName}.java")}package ${class.dtoPackageName};

import java.io.Serializable;

/******************
 * CODE GENERATED BY "${generator.templateName}" - DO NOT EDIT!
 * <br>
 * source: {@link ${class.name}}
<#if class.comment??> * ${class.comment}</#if>
 ******************/
public class ${class.dtoClassName} implements Serializable
{
<#list class.dtoFieldAnnotations as each>
 /**
  * source: ${each.element.name}<#if each.dtoPath??>.${each.dtoPath}</#if>
<#if each.element.comment??>  * ${each.element.comment}
</#if>  **/
    private ${each.dtoFieldType} ${each.dtoName};
</#list>

<#list class.dtoFieldAnnotations as each>
    public ${each.dtoFieldType} <#if each.dtoFieldType="boolean">is<#else>get</#if>${service.capitalize(each.dtoName)}() {
       return ${each.dtoName};
    }

    public void set${service.capitalize(each.dtoName)}(${each.dtoFieldType} ${each.dtoName}) {
        this.${each.dtoName} = ${each.dtoName};
    }
    
</#list>
<#if class.findByElementAnnotation("com.agimatec.annotations.ToString")??>
    @Override
    public String toString() {
      return new StringBuilder()
        .append("{${class.dtoPackageName}.${class.dtoClassName}: ")
<#list class.dtoFieldAnnotations as each>
      <#if each.getElement().getAnnotation("com.agimatec.annotations.ToString")??>
        .append("<#if each_index &gt; 0>,</#if>${each.dtoName}=").append(${each.dtoName})
      </#if>
</#list>
        .append('}').toString();
    }
</#if>
}
</#if></#list>