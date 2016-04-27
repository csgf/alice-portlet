--
-- cloudprovider_db.sql
--
-- Script that creates the cloudprovider database
--
drop database if exists cloudprovider;
create database cloudprovider;
grant all on cloudprovider.* TO 'cloudprovider'@'%' IDENTIFIED BY "cloudprovider_password";
grant all on cloudprovider.* TO 'cloudprovider'@'localhost' IDENTIFIED BY "cloudprovider_password";
use cloudprovider;

-- Cloud provider
-- The cloud provider represents the cloud resurce; it may be inabled/disabled in general or at application level
create table cloud_provider (
    id           int unsigned not null auto_increment
   ,name         varchar(256)
   ,address      varchar(256)
   ,port         int unsigned not null
   ,proto        varchar(32)
   ,enabled      boolean default false
   
   ,primary key(id)
);

insert into cloud_provider(id,name,address,port,proto,enabled) values (1,"nebula-server-01","nebula-server-01.ct.infn.it",9000,"rocci",true);
insert into cloud_provider(id,name,address,port,proto,enabled) values (2,"stack-server-01","stack-server-01.ct.infn.it",8787,"rocci",true);

-- Cloud provider for applications
create table cloud_provider_app (
     ge_operation_id   int unsigned not null -- References GridEngine' GridOperation id
    ,cloud_provider_id int unsigned not null
    ,enabled           boolean default false 
);

insert into cloud_provider_app (ge_operation_id,cloud_provider_id,enabled) values (136,1,true);
insert into cloud_provider_app (ge_operation_id,cloud_provider_id,enabled) values (136,2,false);

-- Each application has its own cloud_provider parameters 
create table cloud_provider_params (
    ge_operation_id   int unsigned not null -- References GridEngine' GridOperation id
   ,cloud_provider_id int unsigned not null
   ,param_name        varchar(64)
   ,param_value       varchar(256)
);

-- aleph2k for nebula-server-01
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,1,"resource","compute");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,1,"action","create");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,1,"attributes_title","aleph2k");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,1,"mixin_os_tpl","uuid_aleph2000_vm_71");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,1,"mixin_resource_tpl","small"); 
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,1,"auth","x509");
-- aleph2k for stack-server-01
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,2,"resource","compute");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,2,"action","create");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,2,"attributes_title","aleph2k");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,2,"mixin_os_tpl","c3484114-9c67-44ff-a3da-ea9e6058fe3b");
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,2,"mixin_resource_tpl","m1-large"); 
insert into cloud_provider_params (ge_operation_id,cloud_provider_id,param_name,param_value) values (136,2,"auth","x509");

-- Infrastructure section
-- Infrastructure are on top of the hierarchy once fixed an infrastructure it hodls:
--  * VO specific settings
--  * List of the supported resources
create table infrastructures (
    id      int unsigned not null auto_increment 
   ,name    varchar(64)
   ,adaptor varchar(64)
   ,enabled boolean default true

   ,primary key(id)
);

insert into infrastructures (id, name, adaptor, enabled) values (1,"GridCT(fedcloud.egi.eu)","rocci",true);
insert into infrastructures (id, name, adaptor, enabled) values (2,"GridCT(vo.chain-project.eu)","rocci",true);

-- Infrastructure params
-- Each infrastructure maintains a list of parameters
create table infrastructure_params (
    infrastructure_id int unsigned not null
   ,param_name  varchar(64)
   ,param_value varchar(128)
);

insert into infrastructure_params (infrastructure_id,param_name,param_value) values (1,"etoken_host","etokenserver.ct.infn.it");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (1,"etoken_port","8082");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (1,"etoken_id","bc779e33367eaad7882b9dfaa83a432c");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (1,"VO","fedcloud.egi.eu");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (1,"VO_GroupRole","fedcloud.egi.eu");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (1,"ProxyRFC","true");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (2,"etoken_host","etokenserver.ct.infn.it");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (2,"etoken_port","8082");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (2,"etoken_id","bc681e2bd4c3ace2a4c54907ea0c379b");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (2,"VO","vo.chain-project.eu");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (2,"VO_GroupRole","vo.chain-project.eu");
insert into infrastructure_params (infrastructure_id,param_name,param_value) values (2,"ProxyRFC","true");

-- Infrastructure resources
-- Link table that maps infrastructures with supported resources (cloud_providers) for that infrastructure (if enabled)
create table infrastructure_resources (
    infrastructure_id int unsigned not null
   ,cloud_provider_id int unsigned not null
   ,enabled boolean default true
);

insert into infrastructure_resources (infrastructure_id,cloud_provider_id) values (1,1);
insert into infrastructure_resources (infrastructure_id,cloud_provider_id) values (1,2);
insert into infrastructure_resources (infrastructure_id,cloud_provider_id) values (2,1);
insert into infrastructure_resources (infrastructure_id,cloud_provider_id) values (2,2);

-- Application infrastructures
-- Each application may support different infrastructures; applications are identified by
-- the GridEngine' id field inside the GridOperations tables
create table infrastructure_apps (
    ge_operation_id   int unsigned not null
   ,infrastructure_id int unsigned not null
   ,enabled           boolean default true
);

insert into infrastructure_apps (ge_operation_id, infrastructure_id) values (136,1);
insert into infrastructure_apps (ge_operation_id, infrastructure_id) values (136,2);
