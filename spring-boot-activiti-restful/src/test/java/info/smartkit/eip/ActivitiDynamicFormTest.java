package info.smartkit.eip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.junit.Rule;
import org.junit.Test;
//@see http://www.kafeitu.me/activiti/2012/08/05/diff-activiti-workflow-forms.html
public class ActivitiDynamicFormTest {
//	private String filename = "/Users/yangboz/Documents/Git/north-american-adventure/RushuMicroService/eip-rushucloud/src/main/resources/test/DynamicForm.bpmn";

	@Rule
	public ActivitiRule activitiRule = new ActivitiRule();

	@Test
	public void startProcess() throws Exception {
		RepositoryService repositoryService = activitiRule
				.getRepositoryService();
		repositoryService
				.createDeployment()
//				.addInputStream("DynamicForm.bpmn20.xml",
//						new FileInputStream(filename))
//						.deploy();
		.addClasspathResource("test/DynamicForm.bpmn")
		.addClasspathResource("test/DynamicForm.png")
		.enableDuplicateFiltering()
		.name("dynamicForm")
		.deploy();

		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionKey("DynamicForm").latestVersion()
				.singleResult();
		FormService formService = activitiRule.getFormService();
		StartFormData startFormData = formService
				.getStartFormData(processDefinition.getId());
		assertNull(startFormData.getFormKey());

		Map<String, String> formProperties = new HashMap<String, String>();
		formProperties.put("name", "YangboZ");

		ProcessInstance processInstance = formService.submitStartFormData(
				processDefinition.getId(), formProperties);
		assertNotNull(processInstance);

		// 运行时变量
		RuntimeService runtimeService = activitiRule.getRuntimeService();
		Map<String, Object> variables = runtimeService
				.getVariables(processInstance.getId());
		assertEquals(variables.size(), 1);
		Set<Entry<String, Object>> entrySet = variables.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			System.out.println(entry.getKey() + "=" + entry.getValue());
		}

		// 历史记录
		HistoryService historyService = activitiRule.getHistoryService();
		List<HistoricDetail> list = historyService.createHistoricDetailQuery()
				.formProperties().list();
		assertEquals(1, list.size());

		// 获取第一个节点
		TaskService taskService = activitiRule.getTaskService();
		Task task = taskService.createTaskQuery().singleResult();
		assertEquals("First Step", task.getName());

		TaskFormData taskFormData = formService.getTaskFormData(task.getId());
		assertNotNull(taskFormData);
		assertNull(taskFormData.getFormKey());
		List<FormProperty> taskFormProperties = taskFormData
				.getFormProperties();
		assertNotNull(taskFormProperties);
		for (FormProperty formProperty : taskFormProperties) {
			System.out
					.println(ToStringBuilder.reflectionToString(formProperty));
		}
		formProperties = new HashMap<String, String>();
		formProperties.put("setInFirstStep", "11/27/2014");
		formService.submitTaskFormData(task.getId(), formProperties);

		// 获取第二个节点
		task = taskService.createTaskQuery().taskName("Second Step")
				.singleResult();
		assertNotNull(task);
		taskFormData = formService.getTaskFormData(task.getId());
		assertNotNull(taskFormData);
		List<FormProperty> formProperties2 = taskFormData.getFormProperties();
		assertNotNull(formProperties2);
		assertEquals(1, formProperties2.size());
		assertNotNull(formProperties2.get(0).getValue());
		assertEquals(formProperties2.get(0).getValue(), "11/28/2014");
	}

}
