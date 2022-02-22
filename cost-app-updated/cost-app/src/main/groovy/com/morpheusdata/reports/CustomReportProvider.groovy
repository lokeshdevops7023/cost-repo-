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
			
				results = new Sql(dbConnection).rows("{CALL vmware_instances_cost_report()}") /*vmware_costs_v1*/
				} else {
				results = new Sql(dbConnection).rows("{CALL vmware_instances_cost_report()}") /*vmware_costs_v1*/
			}
		} finally {
			morpheus.report.releaseDatabaseConnection(dbConnection)
		}
		log.info("Results: ${results}")
		Observable<GroovyRowResult> observable = Observable.fromIterable(results) as Observable<GroovyRowResult>
		observable.map{ resultRow ->
			log.info("Mapping resultRow ${resultRow}")
			/*Map<String,Object> data = [instance_name  : resultRow.instance_name, service_plan_name : resultRow.service_plan_name, max_cores : resultRow.max_cores,max_memory:resultRow.max_memory,max_storage:resultRow.max_storage,tags:resultRow.tags]
		    */
			/*Map<String,Object> data =[per_core_cost : resultRow.per_core_cost,per_gb_memory_cost : resultRow.per_gb_memory_cost,per_gb_storage_cost : resultRow.per_gb_storage_cost,hourly_cost : resultRow.hourly_cost,monthly_cost : resultRow.monthly_cost,instance_name : resultRow.instance_name,instance_id : resultRow.instance_id,service_plan_id : resultRow.service_plan_id,service_plan_name : resultRow.service_plan_name,provision_type : resultRow.provision_type,provision_type_code : resultRow.provision_type_code,max_cores : resultRow.max_cores,max_memory : resultRow.max_memory,max_storage : resultRow.max_storage,cloud_name : resultRow.cloud_name,price_set_code : resultRow.price_set_code,ApplicationName : resultRow.ApplicationName,app_category : resultRow.app_category,backupsolution : resultRow.backupsolution,billing_type : resultRow.billing_type,cloud_category : resultRow.cloud_category,cloud_sub_cat : resultRow.cloud_sub_cat,confidentiality_level : resultRow.confidentiality_level,criticality : resultRow.criticality,csp : resultRow.csp,customer : resultRow.customer,data_locality : resultRow.data_locality,deployed_on : resultRow.deployed_on,environment : resultRow.environment,ESXBackup : resultRow.ESXBackup,hosted_location : resultRow.hosted_location,it_business_tower : resultRow.it_business_tower,mep : resultRow.mep,organization : resultRow.organization,os : resultRow.os,primary_owner : resultRow.primary_owner,project : resultRow.project,secondary_owner : resultRow.secondary_owner,server_role : resultRow.server_role,shutdown_optin_optout : resultRow.shutdown_optin_optout,shutdown_schedule : resultRow.shutdown_schedule,snc_req_create : resultRow.snc_req_create,support_dl : resultRow.support_dl,support_provided_by : resultRow.support_provided_by]		
			*/
			Map<String,Object> data =[instance_name : resultRow.instance_name,service_plan_name : resultRow.service_plan_name,provision_type : resultRow.provision_type,cloud_name : resultRow.cloud_name,max_cores : resultRow.max_cores,max_memory : resultRow.max_memory,max_storage : resultRow.max_storage,per_core_cost : resultRow.per_core_cost,per_gb_memory_cost : resultRow.per_gb_memory_cost,per_gb_storage_cost : resultRow.per_gb_storage_cost,hourly_cost : resultRow.hourly_cost,monthly_cost : resultRow.monthly_cost,price_set_code : resultRow.price_set_code,ApplicationName : resultRow.ApplicationName,app_category : resultRow.app_category,backupsolution : resultRow.backupsolution,billing_type : resultRow.billing_type,cloud_category : resultRow.cloud_category,cloud_sub_cat : resultRow.cloud_sub_cat,confidentiality_level : resultRow.confidentiality_level,criticality : resultRow.criticality,csp : resultRow.csp,customer : resultRow.customer,data_locality : resultRow.data_locality,deployed_on : resultRow.deployed_on,environment : resultRow.environment,ESXBackup : resultRow.ESXBackup,hosted_location : resultRow.hosted_location,it_business_tower : resultRow.it_business_tower,mep : resultRow.mep,organization : resultRow.organization,os : resultRow.os,primary_owner : resultRow.primary_owner,project : resultRow.project,secondary_owner : resultRow.secondary_owner,server_role : resultRow.server_role,shutdown_optin_optout : resultRow.shutdown_optin_optout,shutdown_schedule : resultRow.shutdown_schedule,snc_req_create : resultRow.snc_req_create,support_dl : resultRow.support_dl,support_provided_by : resultRow.support_provided_by]		
			
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
