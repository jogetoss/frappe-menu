package org.joget.marketplace;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormat;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.userview.model.Userview;
import org.joget.apps.userview.model.UserviewMenu;
import org.joget.apps.userview.service.UserviewUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class FrappeMenu extends UserviewMenu implements PluginWebSupport {

    private DataList cacheDataList = null;
    private final static String MESSAGE_PATH = "messages/FrappeMenu";

    @Override
    public String getName() {
        return "Frappe Menu";
    }

    @Override
    public String getVersion() {
        return "8.0.0";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getLabel() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.marketplace.FrappeMenu.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getDescription() {
        //support i18n
        return AppPluginUtil.getMessage("org.joget.marketplace.FrappeMenu.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/FrappeMenu.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String getCategory() {
        return "Marketplace";
    }

    @Override
    public String getIcon() {
        return "<i class=\"fas fa-chart-bar\"></i>";
    }

    @Override
    public boolean isHomePageSupported() {
        return true; // Can use as first page of the userview
    }

    @Override
    public String getDecoratedMenu() {
        return null; // using default
    }

    protected DataList getDataList() throws BeansException {
        if (cacheDataList == null) {
            // get datalist
            ApplicationContext ac = AppUtil.getApplicationContext();
            AppService appService = (AppService) ac.getBean("appService");
            DataListService dataListService = (DataListService) ac.getBean("dataListService");
            DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao) ac.getBean("datalistDefinitionDao");
            String id = getPropertyString("datalistId");
            AppDefinition appDef = appService.getAppDefinition(getRequestParameterString("appId"), getRequestParameterString("appVersion"));
            DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(id, appDef);

            if (datalistDefinition != null) {
                cacheDataList = dataListService.fromJson(datalistDefinition.getJson());

                if (getPropertyString(Userview.USERVIEW_KEY_NAME) != null && getPropertyString(Userview.USERVIEW_KEY_NAME).trim().length() > 0) {
                    cacheDataList.addBinderProperty(Userview.USERVIEW_KEY_NAME, getPropertyString(Userview.USERVIEW_KEY_NAME));
                }
                if (getKey() != null && getKey().trim().length() > 0) {
                    cacheDataList.addBinderProperty(Userview.USERVIEW_KEY_VALUE, getKey());
                }

                cacheDataList.setActionPosition(getPropertyString("buttonPosition"));
                cacheDataList.setSelectionType(getPropertyString("selectionType"));
                cacheDataList.setCheckboxPosition(getPropertyString("checkboxPosition"));
            }
        }
        return cacheDataList;
    }

    @Override
    public String getRenderPage() {
        Map freeMarkerModel = new HashMap();
        freeMarkerModel.put("request", getRequestParameters());
        freeMarkerModel.put("element", this);

        //build filters and datalist table
        String datalistContent = "";
        datalistContent = getDatalistHTML();
        datalistContent = datalistContent.substring(0, datalistContent.length() - 8);

        DataList datalist = getDataList();
        //generate data for gantt chart
        getBinderData(datalist);
        
        //gantt chart - more properties
        //TODO - set to readonly
        
        String customization = getPropertyString("customization");
        
        if(!customization.isEmpty()){
            customization = "," + customization;
            setProperty("customization", customization);
        }
        
        //build chart
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        String content = pluginManager.getPluginFreeMarkerTemplate(freeMarkerModel, getClass().getName(), "/templates/FrappeMenu.ftl", MESSAGE_PATH);

        //combine both
        String combined = "<div id=gantt-body-" + getPropertyString("id") + " class=\"grantt-body-content\">" + datalistContent + content + "</div>";
        return combined;
    }

    protected String getDatalistHTML() {
        Map model = new HashMap();
        model.put("requestParameters", getRequestParameters());

        try {
            // get data list
            DataList dataList = getDataList();

            if (dataList != null) {
                //overide datalist result to use userview result
                DataListActionResult ac = dataList.getActionResult();
                if (ac != null) {
                    if (ac.getMessage() != null && !ac.getMessage().isEmpty()) {
                        setAlertMessage(ac.getMessage());
                    }
                    if (ac.getType() != null && DataListActionResult.TYPE_REDIRECT.equals(ac.getType())
                            && ac.getUrl() != null && !ac.getUrl().isEmpty()) {
                        if ("REFERER".equals(ac.getUrl())) {
                            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
                            if (request != null && request.getHeader("Referer") != null) {
                                setRedirectUrl(request.getHeader("Referer"));
                            } else {
                                setRedirectUrl("REFERER");
                            }
                        } else {
                            if (ac.getUrl().startsWith("?")) {
                                ac.setUrl(getUrl() + ac.getUrl());
                            }
                            setRedirectUrl(ac.getUrl());
                        }
                    }
                }

                // set data list
                setProperty("dataList", dataList);
            } else {
                setProperty("error", "Data List \"" + getPropertyString("datalistId") + "\" not exist.");
            }
        } catch (BeansException ex) {
            StringWriter out = new StringWriter();
            ex.printStackTrace(new PrintWriter(out));
            String message = ex.toString();
            message += "\r\n<pre class=\"stacktrace\">" + out.getBuffer() + "</pre>";
            setProperty("error", message);
        }

        Map properties = getProperties();
        model.put("properties", properties);

        String result = UserviewUtil.renderJspAsString("userview/plugin/datalist.jsp", model);
        return result;
    }

    protected void getBinderData(DataList datalist) {
        DataListCollection binderdata;
        //DataListColumn[] columns;
        
        String idColumn = getPropertyString("mapping_id");
        String titleColumn = getPropertyString("mapping_title");
        String startColumn = getPropertyString("mapping_start");
        String endColumn = getPropertyString("mapping_end");
        
        String progressColumn = getPropertyString("mapping_progress");
        String dependenciesColumn = getPropertyString("mapping_dependencies");
        String customClassColumn =  getPropertyString("mapping_custom_class");

        //TODO - each record set different classname
        
        try {
            if (getPropertyString("chartUseAllDataRows").equalsIgnoreCase("true")) {
                binderdata = datalist.getRows(datalist.getTotal(), 0);
            } else {
                binderdata = datalist.getRows();
            }
            //columns = datalist.getColumns();
        } catch (Exception e) {
            LogUtil.error(FrappeMenu.class.getName(), e, "Not able to retrieve data from binder");
            setProperty("error", ResourceBundleUtil.getMessage("userview.sqlchartmenu.error.invalidData"));
            return;
        }

        if (binderdata != null && !binderdata.isEmpty()) {
            try {
                JSONArray dataCollection = new JSONArray();

                for (Object r : binderdata) {
                    String id = getBinderFormattedValue(datalist, r, idColumn);
                    String title = getBinderFormattedValue(datalist, r, titleColumn);
                    String start = getBinderFormattedValue(datalist, r, startColumn);
                    String end = getBinderFormattedValue(datalist, r, endColumn);
                    
                    String progress = "";
                    if(!progressColumn.isEmpty()){
                        progress = getBinderFormattedValue(datalist, r, progressColumn);
                    }
                    
                    float progressFloat = 0f;
                    if(!dependenciesColumn.isEmpty()){
                        try{
                            progressFloat = Float.parseFloat(progress);
                        }catch(Exception ex){
                            LogUtil.error(FrappeMenu.class.getName(), ex, "Cannot parse [" + progress + "]");
                        }
                    }
                    
                    String dependencies = "";
                    if(!customClassColumn.isEmpty()){
                        dependencies = getBinderFormattedValue(datalist, r, dependenciesColumn);
                    }
                    
                    String customClass = "";
                    if(!dependenciesColumn.isEmpty()){
                        customClass = getBinderFormattedValue(datalist, r, customClassColumn);
                    }
                    
                    JSONObject dataPoint = new JSONObject();
                    //dataPoint.put("id", id.replace("-", ""));
                    dataPoint.put("id", id);
                    dataPoint.put("start", start);
                    dataPoint.put("end", end);
                    dataPoint.put("name", title);
                    dataPoint.put("progress", progressFloat);
                    //dataPoint.put("dependencies", dependencies.replace("-", ""));
                    dataPoint.put("dependencies", dependencies);
                    dataPoint.put("custom_class", customClass);

                    dataCollection.put(dataPoint);
                }
                
                //LogUtil.info(getClassName(), "data > " + dataCollection.toString());
                
                setProperty("data", dataCollection.toString());

            } catch (JSONException e) {
                LogUtil.error(FrappeMenu.class.getName(), e, "Not able to render gantt data");
                setProperty("error", AppPluginUtil.getMessage("userview.Frappe.error.chartRendering", getClassName(), MESSAGE_PATH));
            }
        } else {
            setProperty("error", ResourceBundleUtil.getMessage("userview.processStatus.noData"));
        }
    }

    protected String getBinderFormattedValue(DataList dataList, Object o, String name) {
        DataListColumn[] columns = dataList.getColumns();
        for (DataListColumn c : columns) {
            if (c.getName().equalsIgnoreCase(name)) {
                String value;
                try {
                    value = DataListService.evaluateColumnValueFromRow(o, name).toString();
                    Collection<DataListColumnFormat> formats = c.getFormats();
                    if (formats != null) {
                        for (DataListColumnFormat f : formats) {
                            if (f != null) {
                                value = f.format(dataList, c, o, value);
                                return value;
                            } else {
                                return value;
                            }
                        }
                    } else {
                        return value;
                    }
                } catch (Exception ex) {

                }
            }
        }
        return "";
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String action = request.getParameter("action");
        if ("getDatalistColumns".equals(action)) {
            try {
                ApplicationContext ac = AppUtil.getApplicationContext();
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao) ac.getBean("datalistDefinitionDao");
                DataListService dataListService = (DataListService) ac.getBean("dataListService");

                String datalistId = request.getParameter("id");
                DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(datalistId, appDef);

                DataList datalist;
                if (datalistDefinition != null) {
                    datalist = dataListService.fromJson(datalistDefinition.getJson());
                    DataListColumn[] datalistcolumns = datalist.getColumns();

                    //JSONObject jsonObject = new JSONObject();
                    JSONArray columns = new JSONArray();
                    
                    //add empty column
                    JSONObject column = new JSONObject();
                    column.put("value", "");
                    column.put("label", "");
                    columns.put(column);    
                    
                    for (int i = 0; i < datalistcolumns.length; i++) {
                        column = new JSONObject();
                        column.put("value", datalistcolumns[i].getName());
                        column.put("label", datalistcolumns[i].getLabel());
                        columns.put(column);
                    }
                    
                        
                    columns.write(response.getWriter());
                } else {
                    JSONArray columns = new JSONArray();
                    columns.write(response.getWriter());
                }

            } catch (IOException | JSONException | BeansException e) {
                LogUtil.error(FrappeMenu.class.getName(), e, "Webservice getColumns");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
}
