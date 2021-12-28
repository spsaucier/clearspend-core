update card set status = 'ACTIVE' where status = 'OPEN';
update card set status = 'CANCELLED' where status = 'CLOSED';