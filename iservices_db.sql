--
-- iservices database setup
--
drop database if exists iservices;
create database iservices;
grant all on iservices.* TO 'iservices_user'@'localhost' IDENTIFIED BY "iservices_password";
grant all on iservices.* TO 'iservices_user'@'%'         IDENTIFIED BY "iservices_password";
use iservices;

-- services table
create table services (
	srv_id        int unsigned not null auto_increment
       ,srv_name      varchar(64)                   -- Service name
       ,srv_shdesc    varchar(128)                  -- Service short description
       ,srv_desc      varchar(4096)                 -- Service long description
       ,srv_limit     int unsigned default 1        -- Maximum allowd number of instantiable services by a user
       ,srv_duration  int unsigned default 2592000  -- Service duration in seconds 
       ,primary key (srv_id)
);


-- service values
insert into services (srv_id, srv_name, srv_shdesc, srv_desc) values (1,'ALEPH2k','ALEPH was a particle physics experiment installed on the Large Electron-Positron collider (LEP) at the CERN laboratory in Geneva/Switzerland.','ALEPH was a particle physics experiment installed on the Large Electron-Positron collider (LEP) at the CERN laboratory in Geneva/Switzerland. It was designed to explore the physics predicted by the Standard Model and to search for physics beyond it. ALEPH first measured events in LEP in July 1989. LEP operated at around 91 GeV – the predicted optimum energy for the formation of the Z particle. From 1995 to 2000 the accelerator operated at energies up to 200 GeV, above the threshold for producing pairs of W particles. The data taken, consisted of millions of events recorded by the ALEPH detector,allowed precision tests of the electro-weak Standard Model (SM) to be undertaken. The group here concentrated our analysis efforts mainly in Heavy Flavour (beauty and charm) physics, in searches for the the Higgs boson, the particles postulated to generate particle mass, and for physics beyond the SM, e.g. Supersymmetry, and in W physics.

This application perform the search for the production and non-standard decay of a scalar Higgs boson into four tau leptons through the intermediation of the neutral pseudo-scalars Higgs particle. 

The analysis was conducted by the ALEPH collaboration with the data collected at centre-of-mass energies from 183 to 209 GeV.');

-- access to services
-- Any service has its own access methods such as: 'ssh, vnc, rdp, etc'
-- Each method requires a different kind of credentials that are included into a ; separated string inside  acc_info
-- These settings contain generic information that could be customized duing service allocation (see allocated_access)
create table access_services (
        acc_id           int unsigned not null auto_increment
       ,srv_id           int unsigned not null
       ,acc_type         varchar(32)
       ,acc_info         varchar(4096)
       ,acc_limit        int unsigned default 1
       ,primary key (acc_id)
       ,foreign key (srv_id) references services(srv_id)
);

-- access values
insert into access_services (acc_id,srv_id,acc_type,acc_info) values (1,1,'ssh','username=alephusr;password=alephusr');

-- allocated services
create table allocated_services (
        alloc_id         int unsigned not null auto_increment
       ,srv_id           int unsigned not null
       ,srv_uuid         varchar(36)  not null
       ,portal_user      varchar(128)
       ,alloc_ts         datetime
       ,alloc_expts      datetime
       ,alloc_state      varchar(32)
       ,primary key (alloc_id)
       ,foreign key (srv_id) references services(srv_id)
);

-- access to allocated services
-- Allocated services may have different access methods and credentials
create table allocated_access (
        access_id        int unsigned not null auto_increment
       ,alloc_id         int unsigned not null    
       ,acc_type         varchar(32)
       ,acc_info         varchar(4096)
       ,primary key (access_id)
       ,foreign key (alloc_id) references allocated_services(alloc_id)      
);


