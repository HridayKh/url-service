    create table urls (
        click_count integer not null,
        expiry_max_clicks integer,
        is_active bit not null,
        is_deleted bit not null,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        expiry_inactivity_duration_seconds bigint,
        expiry_time datetime(6),
        id bigint not null auto_increment,
        last_clicked_at datetime(6),
        user_id bigint,
        original_url TEXT not null,
        password_hash varchar(255),
        short_url varchar(255) not null,
        delete_reason enum ('EXPIRED','UNKNOWN','USER_REQUEST','VIOLATION'),
        expiry_type enum ('INACTIVITY','NONE','TIME','USAGE') not null,
        qr_metadata json,
        primary key (id)
    ) engine=InnoDB
Hibernate: 
    create table users (
        is_deleted bit not null,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        id bigint not null auto_increment,
        email varchar(255) not null,
        name varchar(255) not null,
        profile_picture varchar(255),
        primary key (id)
    ) engine=InnoDB
Hibernate: 
    alter table urls 
       add constraint idx_short_url unique (short_url)
Hibernate: 
    alter table users 
       add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email)
Hibernate: 
    alter table users 
       add constraint UK3g1j96g94xpk3lpxl2qbl985x unique (name)
Hibernate: 
    alter table urls 
       add constraint FK31nbxw9e1inas1lmdkwxqv10 
       foreign key (user_id) 
       references users (id)