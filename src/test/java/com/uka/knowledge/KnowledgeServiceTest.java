package com.uka.knowledge;

import com.uka.knowledge.model.dto.KnowledgeNodeDTO;
import com.uka.knowledge.model.dto.KnowledgeRelationDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.service.KnowledgeNodeService;
import com.uka.knowledge.service.KnowledgeRelationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 知识图谱服务测试类
 *
 * @author uka
 * @version 1.0
 */
@SpringBootTest
class KnowledgeServiceTest {

    @Autowired
    private KnowledgeNodeService nodeService;

    @Autowired
    private KnowledgeRelationService relationService;

    /**
     * 测试创建知识节点
     */
    @Test
    void testCreateNode() {
        KnowledgeNodeDTO dto = new KnowledgeNodeDTO();
        dto.setName("测试节点");
        dto.setNodeType("Concept");
        dto.setDescription("这是一个测试节点");

        KnowledgeNode node = nodeService.createNode(dto);

        assertNotNull(node);
        assertNotNull(node.getId());
        assertEquals("测试节点", node.getName());
        assertEquals("Concept", node.getNodeType());

        // 清理测试数据
        nodeService.deleteNode(node.getId());
    }

    /**
     * 测试创建知识关系
     */
    @Test
    void testCreateRelation() {
        // 先创建两个节点
        KnowledgeNodeDTO nodeDto1 = new KnowledgeNodeDTO();
        nodeDto1.setName("测试节点1");
        nodeDto1.setNodeType("Concept");
        KnowledgeNode node1 = nodeService.createNode(nodeDto1);

        KnowledgeNodeDTO nodeDto2 = new KnowledgeNodeDTO();
        nodeDto2.setName("测试节点2");
        nodeDto2.setNodeType("Concept");
        KnowledgeNode node2 = nodeService.createNode(nodeDto2);

        // 创建关系
        KnowledgeRelationDTO relationDto = new KnowledgeRelationDTO();
        relationDto.setName("关联");
        relationDto.setRelationType("RELATED_TO");
        relationDto.setSourceNodeId(node1.getId());
        relationDto.setTargetNodeId(node2.getId());
        relationDto.setWeight(0.8);

        KnowledgeRelation relation = relationService.createRelation(relationDto);

        assertNotNull(relation);
        assertNotNull(relation.getId());
        assertEquals("关联", relation.getName());
        assertEquals(node1.getId(), relation.getSourceNodeId());
        assertEquals(node2.getId(), relation.getTargetNodeId());

        // 清理测试数据
        relationService.deleteRelation(relation.getId());
        nodeService.deleteNode(node1.getId());
        nodeService.deleteNode(node2.getId());
    }

    /**
     * 测试节点搜索
     */
    @Test
    void testSearchNodes() {
        // 创建测试节点
        KnowledgeNodeDTO dto = new KnowledgeNodeDTO();
        dto.setName("搜索测试节点ABC");
        dto.setNodeType("Concept");
        KnowledgeNode node = nodeService.createNode(dto);

        // 搜索
        var results = nodeService.searchByName("ABC");

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertTrue(results.stream().anyMatch(n -> n.getName().contains("ABC")));

        // 清理
        nodeService.deleteNode(node.getId());
    }
}
