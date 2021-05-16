package info.kgeorgiy.ja.polchinsky.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB extends DB implements AdvancedQuery {

    private static final Comparator<Student> STUDENT_BY_NAME_COMPARATOR =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .reversed()
                    .thenComparing(Student::compareTo);

    private <V> Stream<Map.Entry<String, V>> groupByFirstName(Collection<Student> students,
                                                             Collector<Student, ?, V> downstream) {
        return groupBy(students, Student::getFirstName, downstream);
    }

    public String getMostPopularName(Collection<Student> students) {
        return getKeyWithMaxValue(groupByFirstName(students, countingDistinctBy(Student::getGroup)),
                String::compareTo, "");
    }

    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return mapToListByIndices(students, indices, Student::getFirstName);
    }

    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return mapToListByIndices(students, indices, Student::getLastName);
    }

    public List<GroupName> getGroups(Collection<Student> students, int[] indices) {
        return mapToListByIndices(students, indices, Student::getGroup);
    }

    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return mapToListByIndices(students, indices, this::getFullName);
    }

    private <V> Stream<Map.Entry<GroupName, V>> groupByGroup(Collection<Student> students,
                                                             Collector<Student, ?, V> downstream) {
        return groupBy(students, Student::getGroup, downstream);
    }

    private Stream<Group> getSortedGroups(Collection<Student> students, Comparator<Student> comparator) {
        return groupByGroup(students, Collectors.toList())
                .map(e -> new Group(e.getKey(), toSortedList(e.getValue(), comparator)));
    }

    private List<Group> getGroupsBy(Collection<Student> students, Comparator<Student> comparator) {
        return toSortedList(getSortedGroups(students, comparator), Comparator.comparing(Group::getName));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsBy(students, STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsBy(students, Student::compareTo);
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getKeyWithMaxValue(groupByGroup(students, Collectors.counting()),
                GroupName::compareTo, null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getKeyWithMaxValue(groupByGroup(students, countingDistinctBy(Student::getFirstName)),
                Collections.reverseOrder(GroupName::compareTo), null);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToList(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return mapToList(students, Student::getGroup);
    }

    private String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToList(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapToSortedSet(students, Student::getFirstName);
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return toSortedList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return toSortedList(students, STUDENT_BY_NAME_COMPARATOR);
    }

    private <K> List<Student> filterByKeyAndSortByName(Collection<Student> students,
                                                       K key,
                                                       Function<Student, K> keyExtractor) {
        return toSortedList(filterByKey(students, key, keyExtractor), STUDENT_BY_NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterByKeyAndSortByName(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterByKeyAndSortByName(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filterByKeyAndSortByName(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return filterByKey(students, group, Student::getGroup)
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)));
    }
}
