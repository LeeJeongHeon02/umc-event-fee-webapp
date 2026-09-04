alter table members alter column kakao_id drop not null;
alter table members alter column kakao_profile_name drop not null;

alter table members add column login_id varchar(30);
alter table members add column password_hash varchar(100);
alter table members add column phone_number varchar(20);

alter table members add constraint uk_members_login_id unique (login_id);
alter table members add constraint uk_members_phone_number unique (phone_number);
alter table members add constraint ck_members_auth_identity
    check (kakao_id is not null or login_id is not null);
