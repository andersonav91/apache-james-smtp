# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 20180620182802) do

  # These are extensions that must be enabled in order to support this database
  enable_extension "plpgsql"

  create_table "glx_aws_ses_mail_info", force: :cascade do |t|
    t.string "ses_id"
    t.integer "glx_mail_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "glx_machine_statuses", force: :cascade do |t|
    t.integer "initial_status_id"
    t.string "action"
    t.integer "final_status_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "glx_mails", id: :serial, force: :cascade do |t|
    t.integer "gateway", null: false
    t.string "mail_name", limit: 60, null: false
    t.string "to_email", limit: 125, null: false
    t.string "from_email", limit: 125, null: false
    t.datetime "sent_at"
    t.integer "status_id"
  end

  create_table "glx_sendgrid_mail_info", force: :cascade do |t|
    t.string "sendgrid_id"
    t.integer "glx_mail_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "glx_status_logs", force: :cascade do |t|
    t.string "mail_id"
    t.integer "status_from_id"
    t.string "action"
    t.integer "status_to_id"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "glx_statuses", force: :cascade do |t|
    t.string "name"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
  end

  create_table "glx_subscriptions", id: :serial, force: :cascade do |t|
    t.boolean "subscribed", default: true
    t.string "uuid", limit: 36, null: false
    t.string "email", limit: 125, null: false
    t.string "from_email", limit: 125
    t.datetime "created_at"
    t.datetime "unsubscribed_at"
  end

  create_table "james_domain", primary_key: "domain_name", id: :string, limit: 100, force: :cascade do |t|
  end

  create_table "james_mail", primary_key: ["mailbox_id", "mail_uid"], force: :cascade do |t|
    t.bigint "mailbox_id", null: false
    t.bigint "mail_uid", null: false
    t.boolean "mail_is_answered", null: false
    t.integer "mail_body_start_octet", null: false
    t.bigint "mail_content_octets_count", null: false
    t.boolean "mail_is_deleted", null: false
    t.boolean "mail_is_draft", null: false
    t.boolean "mail_is_flagged", null: false
    t.datetime "mail_date"
    t.string "mail_mime_type", limit: 200
    t.bigint "mail_modseq"
    t.boolean "mail_is_recent", null: false
    t.boolean "mail_is_seen", null: false
    t.string "mail_mime_subtype", limit: 200
    t.bigint "mail_textual_line_count"
    t.binary "mail_bytes", null: false
    t.binary "header_bytes", null: false
    t.index ["mail_is_deleted"], name: "i_jms_mil_mail_is_deleted"
    t.index ["mail_is_recent"], name: "i_jms_mil_mail_is_recent"
    t.index ["mail_is_seen"], name: "i_jms_mil_mail_is_seen"
    t.index ["mail_modseq"], name: "i_jms_mil_mail_modseq"
  end

  create_table "james_mail_property", primary_key: "property_id", id: :bigint, default: nil, force: :cascade do |t|
    t.integer "property_line_number", null: false
    t.string "property_local_name", limit: 500, null: false
    t.string "property_name_space", limit: 500, null: false
    t.string "property_value", limit: 1024, null: false
    t.bigint "mailbox_id"
    t.bigint "mail_uid"
    t.index ["property_id"], name: "index_property_msg_id"
    t.index ["property_line_number"], name: "index_property_line_number"
  end

  create_table "james_mail_userflag", primary_key: "userflag_id", id: :bigint, default: nil, force: :cascade do |t|
    t.string "userflag_name", limit: 500, null: false
    t.bigint "mailbox_id"
    t.bigint "mail_uid"
  end

  create_table "james_mailbox", primary_key: "mailbox_id", id: :bigint, default: nil, force: :cascade do |t|
    t.bigint "mailbox_highest_modseq"
    t.bigint "mailbox_last_uid"
    t.string "mailbox_name", limit: 200, null: false
    t.string "mailbox_namespace", limit: 200, null: false
    t.bigint "mailbox_uid_validity", null: false
    t.string "user_name", limit: 200
  end

  create_table "james_mailbox_annotation", primary_key: ["key", "mailbox_id"], force: :cascade do |t|
    t.string "key", limit: 200, null: false
    t.bigint "mailbox_id", null: false
    t.string "value", limit: 255
  end

  create_table "james_max_default_message_count", primary_key: "quotaroot_id", id: :string, limit: 255, force: :cascade do |t|
    t.bigint "value"
  end

  create_table "james_max_default_storage", primary_key: "quotaroot_id", id: :string, limit: 255, force: :cascade do |t|
    t.bigint "value"
  end

  create_table "james_max_user_message_count", primary_key: "quotaroot_id", id: :string, limit: 255, force: :cascade do |t|
    t.bigint "value"
  end

  create_table "james_max_user_storage", primary_key: "quotaroot_id", id: :string, limit: 255, force: :cascade do |t|
    t.bigint "value"
  end

  create_table "james_quota_currentquota", primary_key: "currentquota_quotaroot", id: :string, limit: 255, force: :cascade do |t|
    t.bigint "currentquota_messagecount"
    t.bigint "currentquota_size"
  end

  create_table "james_recipient_rewrite", primary_key: ["domain_name", "user_name"], force: :cascade do |t|
    t.string "domain_name", limit: 100, null: false
    t.string "user_name", limit: 100, null: false
    t.string "target_address", limit: 100, null: false
  end

  create_table "james_subscription", primary_key: "subscription_id", id: :bigint, default: nil, force: :cascade do |t|
    t.string "mailbox_name", limit: 100, null: false
    t.string "user_name", limit: 100, null: false
    t.index ["user_name", "mailbox_name"], name: "u_jms_ptn_user_name", unique: true
  end

  create_table "james_user", primary_key: "user_name", id: :string, limit: 100, force: :cascade do |t|
    t.string "password_hash_algorithm", limit: 100, null: false
    t.string "password", limit: 128, null: false
    t.integer "version"
  end

  create_table "openjpa_sequence_table", id: :integer, limit: 2, default: nil, force: :cascade do |t|
    t.bigint "sequence_value"
  end

  add_foreign_key "james_mail", "james_mailbox", column: "mailbox_id", primary_key: "mailbox_id", name: "james_mail_mailbox_id_fkey", on_delete: :cascade
  add_foreign_key "james_mail_property", "james_mail", column: "mailbox_id", primary_key: "mailbox_id", name: "james_mail_property_mailbox_id_fkey", on_delete: :cascade
  add_foreign_key "james_mail_userflag", "james_mail", column: "mailbox_id", primary_key: "mailbox_id", name: "james_mail_userflag_mailbox_id_fkey", on_delete: :cascade
end
