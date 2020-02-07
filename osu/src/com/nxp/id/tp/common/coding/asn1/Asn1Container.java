package com.nxp.id.tp.common.coding.asn1;

import java.util.Iterator;
import java.util.List;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.Util;

/**
 * Abstract base class for ASN.1 containers (SEQUENCE, SET, ...).
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public abstract class Asn1Container
    extends Asn1Structure implements Iterable<Asn1Encodable> {

  /** contained elements. */
  @SuppressWarnings("checkstyle:visibilitymodifier")
  protected List<Asn1Encodable> members;

  /**
   * Ctor with tag and a list of contained members.
   *
   * @param tag      The container's tag
   * @param children Its members
   */
  public Asn1Container(final HexString tag, final Asn1Encodable... children) {

    this(tag, Util.wrapInList(children));
  }

  /**
   * Ctor with tag and a list of contained members.
   *
   * @param tag      The container's tag
   * @param children Its members
   */
  public Asn1Container(final HexString tag,
                       final List<Asn1Encodable> children) {

    super(tag, HexString.EMPTY);

    members = children;

    for (Asn1Encodable member : members) {
      value = value.append(member.encode());
    }
  }

  /**
   * Returns the contained elements.
   *
   * @return The children
   */
  public List<Asn1Encodable> getChildren() { return members; }

  /**
   * Adds a structure to the container.
   *
   * @param encodable The element to add
   */
  public void add(final Asn1Encodable encodable) {
    members.add(encodable);

    value = value.append(encodable.encode());
  }

  /**
   * Returns the child at the given index.
   *
   * @param index The child's index
   *
   * @return The child
   *
   * @throws IndexOutOfBoundsException On an invalid index
   */
  public Asn1Encodable get(final int index) throws IndexOutOfBoundsException {
    return members.get(index);
  }

  /**
   * Returns the number of children.
   *
   * @return The number of children
   */
  public int size() { return members.size(); }

  @Override
  public Iterator<Asn1Encodable> iterator() {
    return members.iterator();
  }

  @Override public abstract Asn1Encodable clone();

  protected static void checkRootElement(final Asn1Encodable root,
                                         final Class<?> expectedClass)
      throws InvalidAsn1CodingException {
    if (root == null)
      throw new IllegalArgumentException("Root element should not be null");

    if (!expectedClass.isInstance(root)) {
      throw new InvalidAsn1CodingException(
          String.format("Element was expected to be %s but was %s",
                        expectedClass.getName(), root.getClass()));
    }
  }
}
