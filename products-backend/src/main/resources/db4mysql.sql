drop table if exists `user`;
drop table if exists `goods`;
drop table if exists `historygoods`;
drop table if exists `collectgoods`;

create table `user` (
    `user_id` int not null auto_increment,
    `user_name` varchar(63) not null,
    `password` varchar(63) not null,
    `email` varchar(63) not null,
    primary key (`user_id`),
    unique (`user_name`)
) engine=innodb charset=utf8mb4;

create table `goods` (
    `goods_id` int not null auto_increment,
    `sku_id` varchar(127) not null,
    `goods_name` varchar(127) not null,
    `goods_link` varchar(512) not null,
    `img_url` varchar(512) not null,
    `price` decimal(7, 2) not null default 0.00,
    `platform` varchar(63) not null,
    primary key (`goods_id`)
    unique (`sku_id`),
) engine=innodb charset=utf8mb4;

create table `historygoods` (
    `goods_id` int not null auto_increment,
    `sku_id` varchar(127) not null,
    `goods_name` varchar(127) not null,
    `goods_link` varchar(512) not null,
    `img_url` varchar(512) not null,
    `price` decimal(7, 2) not null default 0.00,
    `platform` varchar(63) not null,
    primary key (`goods_id`)
) engine=innodb charset=utf8mb4;

create table `collectgoods` (
    `collect_id` int not null auto_increment,
    `goods_id` int not null,
    `user_id` int not null,
    `sku_id` varchar(127) not null,
    `goods_name` varchar(127) not null,
    `goods_link` varchar(512) not null,
    `img_url` varchar(512) not null,
    `price` decimal(7, 2) not null default 0.00,
    `platform` varchar(63) not null,
    primary key (`collect_id`),
    foreign key (`goods_id`) references `goods`(`goods_id`) on delete cascade on update cascade,
    foreign key (`user_id`) references `user`(`user_id`) on delete cascade on update cascade
) engine=innodb charset=utf8mb4;