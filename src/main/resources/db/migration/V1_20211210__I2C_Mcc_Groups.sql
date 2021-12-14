alter table mcc_group
    add column i2c_mcc_group_ref varchar(16) not null,
    add column business_id uuid not null references business (id);

do $$
declare
	v_business_id uuid;
begin
	for v_business_id in execute 'select id from business'
		loop
			insert into mcc_group
			  (id, i2c_mcc_group_ref, name, mcc_codes, created, updated, version, business_id)
			values
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'UT_MCG_CONFG',  'Utilities', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'RS_MCG_CONFG',  'Retail Stores', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'AV_MCG_CONFG',  'Automobiles and Vehicles', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'MS_MCG_CONFG',  'Miscellaneous Stores', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'SP_MCG_CONFG',  'Service Providers', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'PS_MCG_CONFG',  'Personal Service Providers', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'BS_MCG_CONFG',  'Business Services', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'RR_MCG_CONFG',  'Repair Services', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'AE_MCG_CONFG',  'Amusement and Entertainment', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'SM_MCG_CONFG',  'Professional Services and Membership Organizations', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'GS_MCG_CONFG',  'Government Services', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'CR_MCG_CONFG',  'Clothing Stores', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'CS_MCG_CONFG',  'Contracted Services', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'AL_MCG_CONFG',  'Airlines', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'AR_MCG_CONFG',  'Auto Rentals', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'HM_MCG_CONFG',  'Hotels and Motels', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'TT_MCG_CONFG',  'Transportation', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'TE_MCG_CONFG',  'Travel and Entertainment', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'MSL_MCG_CONFG',  'Miscellaneous', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'WS_MCG_CONFG',  'Wholesale Distributors and Manufacturers', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'OP_MCG_CONFG',  'Mail / Phone Order Providers', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'DG_MCG_CONFG',  'Digital Goods / Media', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'RES_MCG_CONFG',  'Restaurants', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'GAS_MCG_CONFG',  'Gas Stations', '[]', now(), now(), 0, v_business_id),
			  ((SELECT md5(random()::text || clock_timestamp()::text)::uuid), 'EDU_MCG_CONFG',  'Education', '[]', now(), now(), 0, v_business_id);
		end loop;
end $$;

update transaction_limit
set limits = '{"currency":"USD","typeMap":{}}';

update business_limit
set limits = '{"currency":"USD","typeMap":{}}';
