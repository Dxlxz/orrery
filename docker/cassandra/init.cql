create keyspace if not exists orrery WITH REPLICATION = {
  'class' : 'SimpleStrategy',
  'replication_factor' : 1
};

create table if not exists orrery.metadata_aspect_v2 (
  urn                           varchar,
  aspect                        varchar,
  version                       bigint,
  metadata                      text,
  systemmetadata                text,
  createdon                     timestamp,
  createdby                     varchar,
  createdfor                    varchar,
  entity                        varchar,
  primary key ((urn), aspect, version))
with clustering order by (aspect asc, version asc);

insert into orrery.metadata_aspect_v2 (urn, aspect, version, metadata, systemmetadata, createdon, createdby, entity) values(
  'urn:li:corpuser:orrery',
  'corpUserInfo',
  0,
  '{"displayName":"Data Hub","active":true,"fullName":"Data Hub","email":"orrery@linkedin.com"}',
  '{}',
  toTimestamp(now()),
  'urn:li:corpuser:__orrery_system',
  'corpuser'
) if not exists;

insert into orrery.metadata_aspect_v2 (urn, aspect, version, metadata, systemmetadata, createdon, createdby, entity) values(
  'urn:li:corpuser:orrery',
  'corpUserEditableInfo',
  0,
  '{"skills":[],"teams":[],"pictureLink":"https://raw.githubusercontent.com/linkedin/orrery/master/orrery-web-react/src/images/default_avatar.png"}',
  '{}',
  toTimestamp(now()),
  'urn:li:corpuser:__orrery_system',
  'corpuser'
) if not exists;
