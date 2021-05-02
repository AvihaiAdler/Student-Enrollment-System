package twins.item;


import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import twins.dal.ItemDao;
import twins.data.ItemEntity;
import twins.errors.BadRequestException;
import twins.errors.NotFoundException;
import twins.logic.ItemConverter;
import twins.logic.UpdatedItemService;

@Service
public class ItemServiceJpa implements UpdatedItemService{
	private ItemDao itemDao;
	private ItemConverter itemEntityConverter;
	private String space;
	
	public ItemServiceJpa() {
		
	}

	@Autowired
	public void setItemDao(ItemDao itemDao) {
		this.itemDao = itemDao;
	}

	
	@Autowired
	public void setItemEntityConverter(ItemConverter itemEntityConverter) {
		this.itemEntityConverter = itemEntityConverter;
	}
	
	@Value("${spring.application.name:2021b.iftach.avraham}")
	public void setSpace(String space) {
		this.space = space;
	}
	
	@Override
	@Transactional
	public ItemBoundary createItem(String userSpace, String userEmail, ItemBoundary item) {
				
		if(item==null)
			throw new BadRequestException("item can't be null");
		
		if(item.getType() == null || item.getType().isEmpty())
			throw new BadRequestException("item type can't be null or empty.");
		
		if(item.getName() == null || item.getName().isEmpty())
			throw new BadRequestException("item name can't be null or empty.");
		
//		if(item.getItemId()==null)
//			throw new BadRequestException("ItemId can't be null");
//		if(item.getItemId().getSpace()==null)
//			throw new BadRequestException("space (of ItemId) can't be null");
//		if(item.getItemId().getId()==null)
//			throw new BadRequestException("id (of ItemId) can't be null");
		
		String newId = this.space + "_" + UUID.randomUUID().toString();//item.getItemId().getId();
			
		ItemEntity entity = this.itemEntityConverter.toEntity(item);
		entity.setId(newId);
		entity.setTimestamp(new Date());
		entity.setUserSpace(this.space);
		entity.setUserEmail(userEmail);
		entity.setItemSpace(this.space);
		entity = this.itemDao.save(entity);
		
		return this.itemEntityConverter.toBoundary(entity);
	}
	
	@Override
	@Transactional
	public ItemBoundary updateItem(String userSpace, String userEmail, String itemSpace, String itemId,
			ItemBoundary update) {
		if(update==null)
			throw new BadRequestException();
		
//		String id = itemSpace + "_" + itemId;
		Optional<ItemEntity> entityOptional =  itemDao.findById(itemId);
		
		if(!entityOptional.isPresent())
			throw new NotFoundException("Item id " + itemId + " doesn't exist");
		
		ItemEntity entity = entityOptional.get();
		
	
		if(update.getActive()!=null) {
			entity.setActive(update.getActive());
		}
		if(update.getType() != null) {
			entity.setType(update.getType());
		}
		if(update.getName() != null) {
			entity.setName(update.getName());
		}
		if(update.getLocation() != null) {
			entity.setLat(update.getLocation().getLat());
			entity.setLng(update.getLocation().getLng());
		}		
		if(update.getItemAttributes() != null) {
			entity.setItemAttributes(this.itemEntityConverter.fromMapToJson(update.getItemAttributes()));
		}
		
		this.itemDao.save(entity);
		
		return itemEntityConverter.toBoundary(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ItemBoundary> getAllItems(String userSpace, String userEmail) {
		Iterable<ItemEntity>  allEntities = this.itemDao.findAll();

		return StreamSupport
				.stream(allEntities.spliterator(), false) 
				.map(this.itemEntityConverter::toBoundary)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public ItemBoundary getSpecificItem(String userSpace, String userEmail, String itemSpace, String itemId) {	
//		String id = itemSpace + "_" + itemId;
		Optional<ItemEntity> optionalItem = this.itemDao.findById(itemId);

		if(!optionalItem.isPresent())
			throw new NotFoundException("Item id " + itemId + " doesn't exist");
		
		ItemBoundary boundary = itemEntityConverter.toBoundary(optionalItem.get());
		
		return boundary;
	}

	@Override
	public void deleteAllItems(String adminSpace, String adminEmail) {
		this.itemDao.deleteAll();
	}

	@Override
	@Transactional
	public void addChildToItem(String parentId, ItemIdBoundary childIdBoundary) {
		ItemEntity parent = this.itemDao
				.findById(parentId)
				.orElseThrow(()->new NotFoundException("could not find parent by id: " + parentId));
		
		if(childIdBoundary == null || childIdBoundary.getId() == null || childIdBoundary.getSpace() == null)
			throw new BadRequestException("Child Id Boundary is not valid.");
		
		String childId = childIdBoundary.getSpace() + "_" + childIdBoundary.getId();
		ItemEntity child = this.itemDao
				.findById(childId)
				.orElseThrow(()->new NotFoundException("could not find child by id: " + childId));
		
		parent.addChild(child);
		
		this.itemDao.save(parent);
		this.itemDao.save(child);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<ItemBoundary> getAllChildren(String parentId) {
		ItemEntity parent = this.itemDao
				.findById(parentId)
				.orElseThrow(()->new NotFoundException("could not find paernt by id: " + parentId));

		return parent
			.getChildren() 
			.stream() 
			.map(this.itemEntityConverter::toBoundary)
			.collect(Collectors.toList());
	}

	
	@Override
	@Transactional(readOnly = true)
	public List<ItemBoundary> getAllParents(String childId) {
		ItemEntity child = this.itemDao
				.findById(childId)
				.orElseThrow(()->new NotFoundException("could not find paernt by id: " + childId));

		return child
			.getParents()
			.stream() 
			.map(this.itemEntityConverter::toBoundary)
			.collect(Collectors.toList());
	}
	
}
