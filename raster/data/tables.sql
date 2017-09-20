begin;

create table points (
 pk serial primary key,
 name text
);
select AddGeometryColumn('points', 'the_geom', '3785', 'POINT', 2);

insert into points (name, the_geom) values ('alpha', ST_GeomFromEWKT('SRID=3785;POINT(10 -20)'));
insert into points (name, the_geom) values ('beta', ST_GeomFromEWKT('SRID=3785;POINT(19 -10)'));
insert into points (name, the_geom) values ('gamma', ST_GeomFromEWKT('SRID=3785;POINT(12 -6)'));

commit;
