package com.dartcorp.jbossdemo.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.dartcorp.jbossdemo.model.User;
import javax.persistence.NamedQuery;

/**
 * Backing bean for User entities.
 * <p/>
 * This class provides CRUD functionality for all User entities. It focuses
 * purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt> for
 * state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */
@Named
@Stateful
@ConversationScoped
public class UserBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long nb;

    public Long getNb() {
        return this.nb;
    }

    public void setNb(Long nb) {
        this.nb = nb;
    }

    /*
     * Support creating and retrieving User entities
     */
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private User user;

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Inject
    private Conversation conversation;

    @PersistenceContext(unitName = "jbossdemo-persistence-unit", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public String create() {

        this.conversation.begin();
        this.conversation.setTimeout(1800000L);
        return "create?faces-redirect=true";
    }

    public void retrieve() {

        if (FacesContext.getCurrentInstance().isPostback()) {
            return;
        }

        if (this.conversation.isTransient()) {
            this.conversation.begin();
            this.conversation.setTimeout(1800000L);
        }

        if (this.id == null) {
            this.user = this.example;
        } else {
            this.user = findById(getId());
        }
    }

    public String incUnits() {
        this.retrieve();
        if (FacesContext.getCurrentInstance().isPostback()) {
            return "end";
        }

        if (this.conversation.isTransient()) {
            this.conversation.begin();
            this.conversation.setTimeout(1800000L);
        }
        try {
            if (this.id == null) {
                return "search?faces-redirect=true";
            } else {
                this.user = findById(getId());

                this.user.setUnits(this.user.getUnits() + 1);
                this.entityManager.merge(this.user);
                this.retrieve();
                return "search?faces-redirect=true";

            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
            return null;
        }
    }

    public String addUnits() {

        if (FacesContext.getCurrentInstance().isPostback()) {
            return "end";
        }

        if (this.conversation.isTransient()) {
            this.conversation.begin();
            this.conversation.setTimeout(1800000L);
        }
        try {
            if (this.id == null) {
                return "search?faces-redirect=true";
            }
            if (this.id == null) {
                return "search?faces-redirect=true";
            } else if (this.id == 0) {
                return "search?faces-redirect=true";
            } else {
                this.user = findById(getId());
                
                String query = "SELECT units FROM User U WHERE U.id = :user_id";
                Long u = (Long) this.entityManager.createQuery(query).setParameter("user_id", getId()).getSingleResult();
                this.user.setUnits(this.user.getUnits() + getNb());
                this.entityManager.merge(this.user);
                return "addunits?faces-redirect=true";
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
            return null;
        }

    }

    public User findById(Long id) {

        return this.entityManager.find(User.class, id);
    }

    /*
     * Support updating and deleting User entities
     */
    public String update() {
        this.conversation.end();

        try {
            if (this.id == null) {
                this.entityManager.persist(this.user);
                return "search?faces-redirect=true";
            } else {
                this.entityManager.merge(this.user);
                return "view?faces-redirect=true&id=" + this.user.getId();
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
            return null;
        }
    }

    public String delete() {
        this.conversation.end();

        try {
            User deletableEntity = findById(getId());

            this.entityManager.remove(deletableEntity);
            this.entityManager.flush();
            return "search?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(e.getMessage()));
            return null;
        }
    }

    /*
     * Support searching User entities with pagination
     */
    private int page;
    private long count;
    private List<User> pageItems;

    private User example = new User();

    public int getPage() {
        return this.page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return 10;
    }

    public User getExample() {
        return this.example;
    }

    public void setExample(User example) {
        this.example = example;
    }

    public String search() {
        this.page = 0;
        return null;
    }

    public void paginate() {

        CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();

        // Populate this.count
        CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
        Root<User> root = countCriteria.from(User.class);
        countCriteria = countCriteria.select(builder.count(root)).where(
                getSearchPredicates(root));
        this.count = this.entityManager.createQuery(countCriteria)
                .getSingleResult();

        // Populate this.pageItems
        CriteriaQuery<User> criteria = builder.createQuery(User.class);
        root = criteria.from(User.class);
        TypedQuery<User> query = this.entityManager.createQuery(criteria
                .select(root).where(getSearchPredicates(root)));
        query.setFirstResult(this.page * getPageSize()).setMaxResults(
                getPageSize());
        this.pageItems = query.getResultList();
    }

    private Predicate[] getSearchPredicates(Root<User> root) {

        CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
        List<Predicate> predicatesList = new ArrayList<Predicate>();

        String fullName = this.example.getFullName();
        if (fullName != null && !"".equals(fullName)) {
            predicatesList.add(builder.like(builder.lower(root.<String>get("fullName")), '%' + fullName.toLowerCase() + '%'));
        }
        String phoneNumber = this.example.getPhoneNumber();
        if (phoneNumber != null && !"".equals(phoneNumber)) {
            predicatesList.add(builder.like(builder.lower(root.<String>get("phoneNumber")), '%' + phoneNumber.toLowerCase() + '%'));
        }
        long units = this.example.getUnits();
        if (units != 0) {
            predicatesList.add(builder.equal(root.get("units"), units));
        }

        return predicatesList.toArray(new Predicate[predicatesList.size()]);
    }

    public List<User> getPageItems() {
        return this.pageItems;
    }

    public long getCount() {
        return this.count;
    }

    /*
     * Support listing and POSTing back User entities (e.g. from inside an
     * HtmlSelectOneMenu)
     */
    public List<User> getAll() {

        CriteriaQuery<User> criteria = this.entityManager
                .getCriteriaBuilder().createQuery(User.class);
        return this.entityManager.createQuery(
                criteria.select(criteria.from(User.class))).getResultList();
    }

    @Resource
    private SessionContext sessionContext;

    public Converter getConverter() {

        final UserBean ejbProxy = this.sessionContext.getBusinessObject(UserBean.class);

        return new Converter() {

            @Override
            public Object getAsObject(FacesContext context,
                    UIComponent component, String value) {

                return ejbProxy.findById(Long.valueOf(value));
            }

            @Override
            public String getAsString(FacesContext context,
                    UIComponent component, Object value) {

                if (value == null) {
                    return "";
                }

                return String.valueOf(((User) value).getId());
            }
        };
    }

    /*
     * Support adding children to bidirectional, one-to-many tables
     */
    private User add = new User();

    public User getAdd() {
        return this.add;
    }

    public User getAdded() {
        User added = this.add;
        this.add = new User();
        return added;
    }
}
