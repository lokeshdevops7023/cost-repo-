package com.morpheusdata.reports

import com.morpheusdata.core.AbstractReportProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.ReportResult
import com.morpheusdata.model.ReportType
import com.morpheusdata.model.ReportResultRow
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import com.morpheusdata.response.ServiceResponse
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import io.reactivex.Observable;

import java.sql.Connection

@Slf4j
class CustomReportProvider extends AbstractReportProvider {
	Plugin plugin
	MorpheusContext morpheusContext

	CustomReportProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheusContext = context
	}

	@Override
	MorpheusContext getMorpheus() {
		morpheusContext
	}

	@Override
	Plugin getPlugin() {
		plugin
	}

	@Override
	String getCode() {
		'custom-report-cost-usage'
	}

	@Override
	String getName() {
		'Report Cost Usage'
	}

	 ServiceResponse validateOptions(Map opts) {
		 return ServiceResponse.success()
	 }

	/**
	 * Demonstrates building a TaskConfig to get details about the Instance and renders the html from the specified template.
	 * @param instance details of an Instance
	 * @return
	 */
	@Override
	HTMLResponse renderTemplate(ReportResult reportResult, Map<String, List<ReportResultRow>> reportRowsBySection) {
		ViewModel<String> model = new ViewModel<String>()
		model.object = reportRowsBySection
		getRenderer().renderTemplate("hbs/instanceReport", model)
	}

	/**
	 * Allows various sources used in the template to be loaded
	 * @return
	 */
	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		def csp = new ContentSecurityPolicy()
		csp.scriptSrc = '*.jsdelivr.net'
		csp.frameSrc = '*.digitalocean.com'
		csp.imgSrc = '*.wikimedia.org'
		csp.styleSrc = 'https: *.bootstrapcdn.com'
		csp
	}


	void process(ReportResult reportResult) {
		morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.generating).blockingGet();
		Long displayOrder = 0
		List<GroovyRowResult> results = []
		Connection dbConnection
		
		try {
			dbConnection = morpheus.report.getReadOnlyDatabaseConnection().blockingGet()
			if(reportResult.configMap?.phrase) {
				String phraseMatch = "${reportResult.configMap?.phrase}%"
				/*results = new Sql(dbConnection).rows("SELECT id,name,status from instance WHERE name LIKE ${phraseMatch} order by name asc;")*/
				results = new Sql(dbConnection).rows("select ins.name as 'instance_name' ,ins.id as 'Instance Id' ,plan_id as 'Service Plan Id' ,svc.name as 'Service Plan Name' ,pt.name as 'Provision Type' ,ins.max_cores as 'max_cores' ,CONCAT(FLOOR(ins.max_memory / 1024.0 / 1024.0 / 1024.0),'GB') as 'max_memory' ,CONCAT(FLOOR(ins.max_storage / 1024.0 / 1024.0 / 1024.0),'GB') as 'max_storage',GROUP_CONCAT( concat(m_Tag.name, ':', m_Tag.value) ORDER BY m_Tag.name SEPARATOR ';') as 'tags' from instance ins inner join service_plan svc on svc.id = ins.plan_id inner join provision_type pt on pt.id = svc.provision_type_id inner join instance_metadata_tag ins_m_Tag on ins_m_Tag.instance_metadata_id = ins.id inner join metadata_tag m_Tag on m_Tag.id = ins_m_Tag.metadata_tag_id where pt.code = 'vmware' GROUP BY ins.name ,ins.id,plan_id,svc.name,pt.name,ins.max_cores,ins.max_memory order by ins.id desc ")

			} else {
				/*results = new Sql(dbConnection).rows("SELECT id,name,status from instance order by name asc;") */
				results = new Sql(dbConnection).rows("select ins.name as 'instance_name' ,ins.id as 'Instance Id' ,svc.name as 'service_plan_name' ,pt.name as 'Provision Type' ,ins.max_cores as 'max_cores',CONCAT(FLOOR(ins.max_memory / 1024.0 / 1024.0 / 1024.0),'GB') as 'max_memory' ,CONCAT(FLOOR(ins.max_storage / 1024.0 / 1024.0 / 1024.0),'GB') as 'max_storage',GROUP_CONCAT( concat(m_Tag.name, ':', m_Tag.value) ORDER BY m_Tag.name SEPARATOR ';') as 'tags' from instance ins inner join service_plan svc on svc.id = ins.plan_id inner join provision_type pt on pt.id = svc.provision_type_id inner join instance_metadata_tag ins_m_Tag on ins_m_Tag.instance_metadata_id = ins.id inner join metadata_tag m_Tag on m_Tag.id = ins_m_Tag.metadata_tag_id where pt.code = 'vmware' GROUP BY ins.name ,ins.id,plan_id,svc.name,pt.name,ins.max_cores,ins.max_memory order by ins.id desc ")
			}
		} finally {
			morpheus.report.releaseDatabaseConnection(dbConnection)
		}
		log.info("Results: ${results}")
		Observable<GroovyRowResult> observable = Observable.fromIterable(results) as Observable<GroovyRowResult>
		observable.map{ resultRow ->
			log.info("Mapping resultRow ${resultRow}")
			Map<String,Object> data = [instance_name  : resultRow.instance_name, service_plan_name : resultRow.service_plan_name, max_cores : resultRow.max_cores,max_memory:resultRow.max_memory,max_storage:resultRow.max_storage,tags:resultRow.tags]
			ReportResultRow resultRowRecord = new ReportResultRow(section: ReportResultRow.SECTION_MAIN, displayOrder: displayOrder++, dataMap: data)
			log.info("resultRowRecord: ${resultRowRecord.dump()}")
			return resultRowRecord
		}.buffer(50).doOnComplete {
			morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.ready).blockingGet();
		}.doOnError { Throwable t ->
			morpheus.report.updateReportResultStatus(reportResult,ReportResult.Status.failed).blockingGet();
		}.subscribe {resultRows ->
			morpheus.report.appendResultRows(reportResult,resultRows).blockingGet()
		}
	}

	 @Override
	 String getDescription() {
		 return "Provides a Sample Report that lists the cost usage. This Report is not tenant scoped."
	 }

	 @Override
	 String getCategory() {
		 return 'inventory'
	 }

	 @Override
	 Boolean getOwnerOnly() {
		 return false
	 }

	 @Override
	 Boolean getMasterOnly() {
		 return true
	 }

	 @Override
	 Boolean getSupportsAllZoneTypes() {
		 return true
	 }

	 @Override
	 List<OptionType> getOptionTypes() {
		 //[new OptionType(code: 'status-report-search', name: 'Search', fieldName: 'phrase', fieldContext: 'config', fieldLabel: 'Search Phrase', displayOrder: 0)]

	 }
 }
