    <script src="${request.contextPath}/plugin/org.joget.marketplace.FrappeMenu/lib/frappe/frappe-gantt.js"></script>
    <link href="${request.contextPath}/plugin/org.joget.marketplace.FrappeMenu/lib/frappe/frappe-gantt.min.css" rel="stylesheet" />
    
    <style>
        #gantt-${element.properties.id!} {
            height: max-content;
            min-height: 500px;
            overflow: scroll;
        }

        .gantt-container .popup-wrapper{
            top: -100px;
        }

        .gantt-container .grid-header {
            position: static;
        }

        .gantt-container {
            background: white;
            height: ${element.properties.height!};
        }

        .upper-text.current-upper{
            position: absolute;
            top: 15px !important;
        }

        button.today-button {
            position: relative !important;
            top: -60px !important;
            right: 0 !important;
            line-height: 15px !important;
        }

        .gantt-container .side-header{
            position: static;
        }

        <#if element.properties.showTable! != "bottom" && element.properties.showTable! != "top">
        #gantt-body-${element.properties.id!} .dataList .table-wrapper, #gantt-datalist-${element.properties.id!} .pagebanner, #gantt-datalist-${element.properties.id!} .pagelinks, #gantt-datalist-${element.properties.id!} .exportlinks{
            display: none !important;
        }
        </#if>

        <#if element.properties.showFilter! != "true">
        #gantt-body-${element.properties.id!} .filter_form{
            display: none !important;
        }
        </#if>

        <#if element.properties.showExportLinks! != "true">
        #gantt-body-${element.properties.id!} .exportlinks{
            display: none !important;
        }
        </#if>

        /* remove border in progressive theme*/
        body:not(#login) #content > main > .datalist-body-content:not(.quickEdit) {
            border: none;
            box-shadow: none;
        }
    </style>
    
    

    <#if element.properties.error! != "">
        ${element.properties.error!}
    </#if>
    


    <div id="gantt-${element.properties.id!}-mode" class="mx-auto mt-3 btn-group" role="group">
        <button type="button" class="btn btn-sm btn-light <#if element.properties.viewMode! == "Quarter Day">active</#if>">Quarter Day</button>
        <button type="button" class="btn btn-sm btn-light <#if element.properties.viewMode! == "Half Day">active</#if>">Half Day</button>
        <button type="button" class="btn btn-sm btn-light <#if element.properties.viewMode! == "Day">active</#if>">Day</button>
        <button type="button" class="btn btn-sm btn-light <#if element.properties.viewMode! == "Week">active</#if>">Week</button>
        <button type="button" class="btn btn-sm btn-light <#if element.properties.viewMode! == "Month">active</#if>">Month</button>
        <button type="button" class="btn btn-sm btn-light <#if element.properties.viewMode! == "Year">active</#if>">Year</button>
    </div>

    <div id="gantt-${element.properties.id!}" style="min-height: ${element.properties.height!}; max-width: ${element.properties.width!}">
    </div>


    ${element.properties.customChartFooter!}

    <script>
        // Function to initialize the Gantt chart
        var gantt;

        function initializeGantt(target, data) {
            if ($(target).data("gantt") != undefined ) {
                g = $(target).data("gantt");
                g.clear();
                $(target).data("gantt", null);
                $(target).html("");
                
            }
            gantt = new Gantt(target, data, {
                view_mode: '${element.properties.viewMode!}',
                date_format: '${element.properties.dateFormat!}'
                ${element.properties.customization!}
            });
            
            $(target).data("gantt", gantt); //set instance into data for easy access from outside / developer console
        }
        
        var data = ${element.properties.data!};
        var target = "#gantt-${element.properties.id!}";
        //initializeGantt(target, data);
        
        $(document).on("page_loaded", function(){
            initializeGantt(target, data);

            $(target + "-mode.btn-group").on("click", "button", function() {
                $btn = $(this);
                var mode = $btn.text();
                
                gantt = $("#gantt-${element.properties.id!}").data("gantt");

                gantt.change_view_mode(mode);
                $btn.parent().find('button').removeClass('active');
                $btn.addClass('active');
            });
        });

        <#if element.properties.showTable! == "bottom">
        $("#gantt-body-${element.properties.id!} .dataList").detach().appendTo("#gantt-body-${element.properties.id!}");
        </#if>
        
    </script>