# This file should contain all the record creation needed to seed the database with its default values.
# The data can then be loaded with the rails db:seed command (or created alongside the database with db:setup).
#
# Examples:
#
#   movies = Movie.create([{ name: 'Star Wars' }, { name: 'Lord of the Rings' }])
#   Character.create(name: 'Luke', movie: movies.first)

connection = ActiveRecord::Base.connection()
connection.execute("TRUNCATE TABLE #{GlxStatus.table_name} RESTART IDENTITY")
connection.execute("TRUNCATE TABLE #{GlxMachineStatus.table_name} RESTART IDENTITY")

# statuses for the emails

status_0 = GlxStatus.create(
    name: "All"
);

status_1 = GlxStatus.create(
    name: "Relayed"
);

status_2 = GlxStatus.create(
    name: "Limited"
);

status_3 = GlxStatus.create(
    name: "Opened"
);

status_4 = GlxStatus.create(
    name: "Delivered"
);

status_5 = GlxStatus.create(
    name: "Bounced"
);

status_6 = GlxStatus.create(
    name: "Processed"
);

status_7 = GlxStatus.create(
    name: "Dropped"
);

# update status_id for glx_mails

GlxMail.where("status_id IS NULL").update_all(status_id: status_1.id)

# records for status machine

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_1.id,
    action:  "Relayed"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_2.id,
    action:  "Limited"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_3.id,
    action:  "Openend"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_3.id,
    action:  "Open"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_4.id,
    action:  "Delivered"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_5.id,
    action:  "Bounce"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_6.id,
    action:  "Processed"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_7.id,
    action:  "Dropped"
);

GlxMachineStatus.create(
    initial_status_id: status_0.id,
    final_status_id: status_4.id,
    action:  "Delivery"
);