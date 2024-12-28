drop table if exists `user`;
drop table if exists `goods`;

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
    `sku_id` int not null,
    `goods_name` varchar(127) not null,
    `goods_link` varchar(127) not null,
    `img_url` varchar(127) not null,
    `price` decimal(7, 2) not null default 0.00,
    `platform` varchar(63) not null,
    primary key (`goods_id`)
) engine=innodb charset=utf8mb4;

create table `borrow` (
  `card_id` int not null,
  `book_id` int not null,
  `borrow_time` bigint not null,
  `return_time` bigint not null default 0,
  primary key (`card_id`, `book_id`, `borrow_time`),
  foreign key (`card_id`) references `card`(`card_id`) on delete cascade on update cascade,
  foreign key (`book_id`) references `book`(`book_id`) on delete cascade on update cascade
) engine=innodb charset=utf8mb4;